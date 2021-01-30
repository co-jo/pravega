phase = "Starting Phase "

num = 0

with open('results.log') as f:
    for line in f:
        num += 1
        if "ERROR" in line or phase in line:
            print (num, line)
