#!/usr/bin/env bash

# Copyright (c) Dell Inc., or its subsidiaries. All Rights Reserved.
# 
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0

set -euo pipefail

# Kubernetes State
DEPLOYMENT_NAME="pravega-logs"
CONFIG_MAP_NAME="pravega-logrotate"
PVC_NAME=pravega-log-sink
VOLUME_NAME=logs

NAMESPACE=${NAMESPACE:-"default"}
IMAGE_REPO=${IMAGE_REPO:-"fluent/fluent-bit"}

HOST_LOGS=host-logs
MOUNT_PATH=/data
CONFIG_MAP_DATA=/etc/config
NAME=${NAME:-"pravega-fluent-bit"}

# Configurable flag parameters.
KEEP_PVC=false
STORAGE_CLASS=${STORAGE_CLASS:-"standard"}
PVC_CAPACITY_GI=${PVC_CAPACITY_GI:-50}
PVC_RECLAIM_PERCENT=${PVC_RECLAIM_PERCENT:-25}
PVC_RECLAIM_TRIGGER=${PVC_RECLAIM_TRIGGER:-95}
LOG_ROTATE_INTERVAL_SECONDS=${LOG_ROTATE_INTERVAL:-10}
# The location on the underlying node where the container logs are stored.
HOST_LOGS_PATH=${HOST_LOGS_PATH:-""}

LOG_ROTATE_THRESHOLD_BYTES=${LOG_ROTATE_THRESHOLD_BYTES:-10000000}
LOG_ROTATE_OLD_DIR=${LOG_ROTATE_OLD_DIR:-"."}
LOG_ROTATE_CONF_PATH=$CONFIG_MAP_DATA/"logrotate.conf"
LOG_EXT="gz"

####################################
#### Flags/Args Parsing
####################################

# Flags must be parsed before any of the heredocs are expanded into variables.
CMD=$1
shift

for i in "$@"; do
    case $i in
        # Options.
        -c=* | --pvc-capacity=*)
            PVC_CAPACITY_GI="${i#*=}"
            ;;
        -r=* | --pvc-reclaim-percent=*)
            PVC_RECLAIM_PERCENT="${i#*=}"
            ;;
        -t=* | --pvc-reclaim-trigger=*)
            PVC_RECLAIM_TRIGGER="${i#*=}"
            ;;
        -h=* | --host-path=*)
            HOST_LOGS_PATH="${i#*=}"
            ;;
        -s=* | --storageclass=*)
            STORAGE_CLASS="${i#*=}"
            ;;
        -i=* | --rotation-interval=*)
            LOG_ROTATE_INTERVAL_SECONDS="${i#*=}"
            ;;
        # Flags.
        -k | --keep-pvc)
            KEEP_PVC=true
            ;;
        *)
            echo -e "\n${i%=*} is an invalid option, or is not provided a required value.\n"
            usage
            exit
            ;;
    esac
done


#################################
# Fluent Bit Configuration
#################################

FLUENT_BIT_INPUTS=$(cat << EOF
[INPUT]
    Name tail
    Path /var/log/containers/*.log
    Parser docker_no_time
    Tag kube.*
    Mem_Buf_Limit 5MB
    Skip_Long_Lines Off

EOF
)

# Two levels of escaping are required: one for the assignment to this variable,
# and another during expansion in the --set flag.
#
# Regex field of 'trim_newline' must be grouped (?<group>).
FLUENT_BIT_PARSERS=$(cat << EOF
[PARSER]
    Name docker_no_time
    Format json
    Time_Keep Off
    Time_Key time
    Time_Format %Y-%m-%dT%H:%M:%S.%L

[PARSER]
    Name trim_newline
    Format regex
    Regex (?<log>[^\\\n]*)

EOF
)

FLUENT_BIT_FILTERS=$(cat << EOF
[FILTER]
    Name parser
    Match *
    Key_Name log
    Parser trim_newline

EOF
)

FLUENT_BIT_OUTPUTS=$(cat << EOF
[OUTPUT]
    Name file
    Match *
    Path $MOUNT_PATH
    Format template
    Template {log}

EOF
)

FLUENT_BIT_SERVICE=$(cat << EOF
[SERVICE]
    Flush 1
    Daemon Off
    Log_Level info
    Parsers_File parsers.conf
    Parsers_File custom_parsers.conf
    HTTP_Server On
    HTTP_Listen 0.0.0.0
    HTTP_Port {{ .Values.service.port }}

EOF
)

#################################
# Log Rotation Configuration
#################################

LOG_ROTATE_CONF=$(cat << EOF
"$MOUNT_PATH/*.log" {
    compress
    copytruncate
    size $LOG_ROTATE_THRESHOLD_BYTES
    rotate 1000
    dateext
    dateformat -%s
}
EOF
)

# Escape all the non-configurable variables to avoid unintended command substitutions or variables expansions.
LOG_ROTATE_WATCH=$(cat << EOF
#!/bin/ash

mebibyte=$((1024**2))

used_kib() {
    echo \$(du -s $LOG_ROTATE_OLD_DIR | cut -f 1)
}

# Makes the assumption that rate of deletion will be never be lower than rate of accumulation.
reclaim() {
    start_kib=\$(used_kib)
    total_kib=\$(($PVC_CAPACITY_GI * \$mebibyte))
    threshold_kib=\$(((\$total_kib * $PVC_RECLAIM_TRIGGER)/100))
    if [ \$start_kib -lt \$threshold_kib ]; then
        return 0
    fi
    target_kib=\$((\$start_kib - (\$total_kib * $PVC_RECLAIM_PERCENT)/100))
    files=\$(ls -tr $LOG_ROTATE_OLD_DIR | grep .$LOG_EXT)
    for file in \$files; do
        if [ \$(used_kib) -gt \$target_kib ]; then
            rm $LOG_ROTATE_OLD_DIR/\$file
        else
            break
        fi
    done
    end_kib=\$(used_kib)
    if [ \$start_kib -gt \$end_kib ]; then
        kib=\$((start_kib - end_kib))
        echo "Reclaimed a total of \$((\$start_kib/\$mebibyte))GiB (\${start_kib}KiB). Used: \$end_kib Total: \$total_kib"
    fi
}

# This function assumes the '-%s' dateformat will be applied. It transforms any files in the '$LOG_ROTATE_OLD_DIR'
# directory in the '<logname>.log-<epoch>.gz' format to '<logname>-<utc>.log.gz'.
rename() {
  suffix=".log"
  rotated=\$(stat $MOUNT_PATH/$LOG_ROTATE_OLD_DIR/*.$LOG_EXT -t -c=%n | sed  's/=//')

  for file in \$rotated; do
      match=\$(echo \$file | grep -oE "\-[0-9]+\.$LOG_EXT\$")
      if [ \$? -eq 0 ]; then
          epoch="\$(echo \$match | grep -oE '[0-9]+')"
          utc=\$(date --utc +%FT%TZ -d "@\$epoch")
          original=\${file%\$match}
          mv \$file "\${original%\$suffix}-\$utc\$suffix.$LOG_EXT"
      fi
  done
}

# Permissions of containing directory changed to please logrotate.
chmod o-wx .
mkdir -p $MOUNT_PATH/$LOG_ROTATE_OLD_DIR

while true; do
    output=\$(stat $MOUNT_PATH/*.log -t -c=%s | sed s/=//)
    for size in \$output; do
        if [ \$size -gt $LOG_ROTATE_THRESHOLD_BYTES ]; then
            logrotate $LOG_ROTATE_CONF_PATH
            # Logrotate does not support a convenient date extension when rotation happens frequently.
            rename
            break
        fi
    done;
    reclaim
    sleep $LOG_ROTATE_INTERVAL_SECONDS
done

EOF
)

apply_logrotate_configmap() {
    tab='    '
    # Apply required indentation by prepending two tabs.
    log_rotate_watch=$(echo "$LOG_ROTATE_WATCH" | sed "s/^/$tab$tab/")
    log_rotate_conf=$(echo "$LOG_ROTATE_CONF" | sed "s/^/$tab$tab/")
    cat << EOF | kubectl apply -n=$NAMESPACE --wait -f -
apiVersion: v1
kind: ConfigMap
metadata:
  name: $CONFIG_MAP_NAME
  namespace: $NAMESPACE
data:
    watch.sh: |
$log_rotate_watch
    logrotate.conf: |
$log_rotate_conf
EOF
}

# Mount the above configmap to provide the logrotate conf and watch/naming functionality.
# Also mount the $PVC_NAME PVC to provide an entry point into the logs.
apply_logrotate_deployment() {
  cat << EOF | kubectl apply -n=$NAMESPACE --wait -f -
apiVersion: apps/v1
kind: Deployment
metadata:
  name: $DEPLOYMENT_NAME
spec:
  replicas: 1
  selector:
    matchLabels:
      name: $DEPLOYMENT_NAME
  template:
    metadata:
      labels:
        name: $DEPLOYMENT_NAME
    spec:
      containers:
      - name: alpine
        image: alpine
        workingDir: $MOUNT_PATH
        command: [ '/bin/ash', '-c' ]
        args:
          - apk add logrotate;
            $CONFIG_MAP_DATA/watch.sh
        volumeMounts:
        - name: pravega-logs
          mountPath: $MOUNT_PATH
        - name: logrotate
          mountPath: $CONFIG_MAP_DATA
      volumes:
      - name: logrotate
        configMap:
          name: $CONFIG_MAP_NAME
          defaultMode: 0700
      - name: pravega-logs
        persistentVolumeClaim:
          claimName: $PVC_NAME
EOF
}

####################################
#### Main Process
####################################

# Kubernetes produces a log file for each pod (/var/log/containers/*). Each line
# of stdout generated by the application is appended to it's respective log file,
# formatted based on the configured 'logging driver' (json-file by default).
#
#   {"log":"...\n","stream":"...","time":"..."}
#
# The fluent-bit transformation process is as follows:
#
# 0. The Kubernetes filter default was removed, preventing various metadata being attached to the event.
# 1. The 'docker_no_time' parser drops the time key and converts it into the following message:
#       {"log": "...\n", "stream":"..."}
# 2. The 'parser' filter applies the 'trim_newline' parser, trimming the newline and overrides the log key.
#       {"log": "...", "stream": "..."}
#    This was done to compensate for the newline that is appended to each line in the output plugin.
# 3. Finally the file output plugin uses the '{log}' template to only write back the contents of the log key,
#    effectively avoiding all the extra metadata that was added through this pipeline.
#       "..."

install() {
    # The claim used to persist the logs. Required for all installations.
    cat << EOF | kubectl apply --wait -n=$NAMESPACE -f -
kind: PersistentVolumeClaim
apiVersion: v1
metadata:
  name: $PVC_NAME
spec:
  storageClassName: $STORAGE_CLASS
  accessModes:
    - ReadWriteMany
  resources:
     requests:
       storage: ${PVC_CAPACITY_GI}Gi
EOF

    args=(
        --set config.service="$FLUENT_BIT_SERVICE"
        --set config.outputs="$FLUENT_BIT_OUTPUTS"
        --set config.filters="$FLUENT_BIT_FILTERS"
        --set config.inputs="$FLUENT_BIT_INPUTS"
        --set config.customParsers="$FLUENT_BIT_PARSERS"
        --set extraVolumeMounts[0].name=$VOLUME_NAME
        --set extraVolumeMounts[0].mountPath=$MOUNT_PATH
        --set extraVolumes[0].name=$VOLUME_NAME
        --set extraVolumes[0].persistentVolumeClaim.claimName=$PVC_NAME
    )

    # In the case where container logs are not stored/forwarded to the default
    # directory (/var/lib/docker/containers), mounting the location where they are is required.
    if [ ! -z $HOST_LOGS_PATH ]; then
        args+=(--set extraVolumeMounts[1].name=$HOST_LOGS)
        args+=(--set extraVolumeMounts[1].mountPath=$HOST_LOGS_PATH)
        args+=(--set extraVolumeMounts[1].readOnly=true)
        args+=(--set extraVolumes[1].name=$HOST_LOGS)
        args+=(--set extraVolumes[1].hostPath.path=$HOST_LOGS_PATH)
        args+=(--set extraVolumes[1].hostPath.type=)
    fi

    helm install $NAME fluent/fluent-bit "${args[@]}" \
        -n=$NAMESPACE \
        --set=image.repository=$IMAGE_REPO
    apply_logrotate_configmap
    apply_logrotate_deployment
}

uninstall() {
    set -e
    local response=$(helm delete $NAME -n=$NAMESPACE)
    local return_status=$?
    # If 'helm delete' fails, do not force an error response if it contains 'not found'.
    if [ $return_status -eq 1 ] && [[ ! $response =~ "not found" ]]; then
        echo $response
        exit 1
    fi
    kubectl delete deployment $DEPLOYMENT_NAME -n=$NAMESPACE --ignore-not-found
    kubectl delete configmap $CONFIG_MAP_NAME -n=$NAMESPACE --ignore-not-found
    if [ $KEEP_PVC = false ]; then
        kubectl delete pvc $PVC_NAME -n=$NAMESPACE --ignore-not-found=true
    fi
    set +e
}

usage() {
        echo -e "Usage: $0 <install, uninstall>"
        echo -e "-h=*|--host-path=*:           Creates a new mount point to provide access to the logs on the host node. default: ''"
        echo -e "                              Specify this when the logs on the host node are not stored at: /var/lib/docker/container/..."
        echo -e "-s=*|--storageclass=*:        The storageclass used to provision the PVC. default: standard"
        echo -e "-c=*|--pvc-capacity=*:        The size of the PVC (in GiB) to provision. default: 50"
        echo -e "-r=*|--pvc-reclaim-percent=*: The percent of space on the PVC to reclaim upon a reclaimation attempt. default: 25"
        echo -e "-t=*|--pvc-reclaim-trigger=*: The percent utilization upon which to trigger a reclaimation. default: 95"
        echo -e "-i=*|--rotation-interval=*:   The interval (in seconds) at which to run the rotation. default: 10"
        echo -e "-k  |--keep-pvc:              Does not remove the existing PVC during uninstallation (retain old logs). default: disabled"
}


case $CMD in
    install)
        install
        ;;
    uninstall)
        uninstall
        ;;
    *)
        echo -e "$CMD in an invalid command.\n"
        usage
        exit
        ;;
esac
