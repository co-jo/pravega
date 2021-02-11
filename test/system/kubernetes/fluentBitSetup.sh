#!/bin/bash

STORAGE_CLASS=standard
STORAGE_CAPACITY_GI=50

PVC_NAME=log-sink
VOLUME_NAME=logs
MOUNT_PATH=/data

HOST_LOGS=host-logs
HOST_LOGS_PATH=${$1:-""}

FLUENT_BIT_FILE_OUTPUT=$(cat << EOF
[OUTPUT]
    Name file
    Match *
    Path /data
EOF
)
if [ $(program_is_installed helm) == 0 ]; then
  echo "helm is not installed, please ensure helm with version >=v3.2.1"
  exit 1
fi

# The claim used to persist the logs. Required for all installations.
cat << EOF | kubectl apply -f -
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
       storage: ${STORAGE_CAPACITY_GI}Gi
EOF

args=(
    --set config.outputs="$FLUENT_BIT_FILE_OUTPUT"
    --set extraVolumeMounts[0].name=$VOLUME_NAME
    --set extraVolumeMounts[0].mountPath=$MOUNT_PATH
    --set extraVolumes[0].name=$VOLUME_NAME
    --set extraVolumes[0].persistentVolumeClaim.claimName=$PVC_NAME
)

# In the case where container logs are not stored in the default directory (/var/lib/docker/containers),
# mounting the location where they are stored (on the host node) is required to access them from the fluent-bit pods.
if [ ! -z $HOST_LOGS_PATH ]; then
    args+=(--set extraVolumeMounts[1].name=$HOST_LOGS)
    args+=(--set extraVolumeMounts[1].mountPath=$HOST_LOGS_PATH)
    args+=(--set extraVolumeMounts[1].readOnly=true)
    args+=(--set extraVolumes[1].name=$HOST_LOGS)
    args+=(--set extraVolumes[1].hostPath.path=$HOST_LOGS_PATH)
    args+=(--set extraVolumes[1].hostPath.type=)
fi

helm install pravega-fluent-bit fluent/fluent-bit "${args[@]}"