docker build --build-arg MODULE=module-api    -t module-api .
docker build --build-arg MODULE=module-batch  -t module-batch .
docker build --build-arg MODULE=module-realtime -t module-realtime .