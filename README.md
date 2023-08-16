# Flasma


## Spec
- **Java 11 (Java 1.8 Support)**
- **Scala 2.12.15**
- **SBT 1.6.2**
- **logback-classic 1.2.10**
- **akka-actor-typed 2.6.8**
- **akka-stream 2.6.8**
- **akka-http 10.2.7**
- **akka-http-caching 10.2.7**
- **akka-http-spray-json 10.2.7**
- **tensorflow-core-platform-gpu 0.4.1**

benchmark
----------------------------------------------------------------
```
CPU : Ryzen 9 3950X
GPU : Nvidia Geforce rtx 3090 24GB (495.29.05)
ENV : docker.io/nvidia/cuda:11.3.1-cudnn8-runtime-ubuntu18.04
```


```
number of target vectors = 100,000
table value = RPS(request per second)
table value 0 -> Cuda Out Of Memory

| batch/dim |  100 |  300 |  512 | 768 | 1024 | 2048 | 30522 |
|-----------|-----:|-----:|-----:|----:|-----:|-----:|------:|
|         1 |  393 |  389 |  383 | 365 |  355 |  192 |    31 |
|         2 |  822 |  705 |  555 | 432 |  394 |  241 |    42 |
|         4 | 1420 |  968 |  684 | 505 |  459 |  260 |     0 |
|         8 | 2187 | 1242 |  836 | 605 |  506 |    0 |     0 |
|        16 | 3006 | 1442 |  936 | 655 |    0 |    0 |     0 |
|        32 | 3750 | 1579 | 1001 |   0 |    0 |    0 |     0 |
|        64 | 4184 | 1645 |    0 |   0 |    0 |    0 |     0 |
|       128 | 4424 |    0 |    0 |   0 |    0 |    0 |     0 |

```


## Introduction

This project is centered around using tensorflow-java to accelerate computation with GPUs in a jvm environment and utilizing it as a service api.
All implementations have been chosen to be the most naive and focus on demonstrating the use of GPUs and Dynamic Batch, as well as a simple implementation in practice.

Cosine Similarity has many uses, as it is typically computed over the eigenvalues of unit vectors given in space, with each distance having a value between -1 and 1, and is generally considered a similarity value between vectors. By definition, this is a brute force type of computation that requires an operation with every target vector. There is no way to approximate this very effectively, so for large scale computations or service points, it is best practice to use an ANN (Approximate Nearest Neighbor) to increase computational performance at the expense of recall.

### ANN method has the following problems.

- **It requires an initial build time for the vectors to be computed for similarity.**
- **When extracting the top k similarities, the approximation performance decreases for values of k above a certain level.**
- **Approximation performance decreases significantly as the dimensionality of the vector increases, i.e., if it is more than 100 to 256 dimensions when using an ANN, proper dimensionality reduction is required.**
- **When the number of digits in the vector is less than 100,000, ANNs do not have a significant computational performance gain due to their structure.**
- **This means that if you are dealing with relatively high dimensional vectors and need a large top k, or if there are not enough target vectors, ANNs will be less useful.**

### In this project, we address the problem as follows.

- **The target vectors are fixed in constant memory on the GPU by dynamically generating a model graph.**
- **The inner operations, represented by metric operations, are dynamically batched with tensor operations in the Tensorflow Graph to process many at once.**
- **Pre-processes and stores and loads L2norm and Transpose operations in advance to avoid unnecessary runtime operations.**
- **Implement Dynamic Batch through akka-http, akka-stream, and asynchronous processing of Akka Http to process hundreds to thousands of requests simultaneously.**

### Achieve the following performance and advantages over traditional best practices and SOTA.

- **Gain approximately 55 to 65% request per second (RPS) without sacrificing recall compared to SOTA (ScaNN, 0.9876) for http://ann-benchmarks.com.** 
- **Loads in less than 2 seconds versus SOTA (ScaNN, 182 seconds) on the glove-100-angular benchmark dataset and spins up servers in less than 5 seconds when deployed.**
- **For a 100,000-level vector, we get between 4000 and 260 requests per second (RPS) for 100 to 2048 dimensions.**
- **Target vectors can be loaded as npy files via python's numpy format.**
- **It uses the tensorflow runtime which is built for multiple environments, so it can be easily used on linux, windows, mac, etc.**
- **We recommend using examples in relatively small production environments to consider throughput, latency, and to simplify the deployment pipeline without reducing recall.**

### Caveats. 
- **Comparison with ann-benchmarks is a lossless calculation with a Recall of 1 and measured with end2end of the REST API, not batch library calls.**
- **Comparisons to ann-benchmarks are not a fair comparison. ann-benchmarks were measured on a CPU r5.4xlarge on AWS, which is a very different environment than the GPU in the current example.**
- **Numerical errors may be caused by implicit GEMM algorithm changes due to the behavior of cublas' MatmulAlgoGetHeuristic in dynamic batch situations.**
- **The maximum available Dynamic Batch size depends on the specifications of the GPU memory. In general, giving it as large a value as your memory allows will result in higher RPS performance.**


## Default Configuration
- **Minimal code**, **Minimal dependencies**.
- Use **Tensorflow-java-gpu** as the Serving Runtime
- Configure the REST API via **AKKA-HTTP**.
- Implementing dynamic batching via **akka-stream**.
- Accelerate JVM GPU computation through tensorflow-java dynamic graph generation and serving


## docker
```
docker build . -f Dockerfile -t flasma:0.1
docker run --gpus all -p 8080:8080 flasma:0.1
```

## local build & run
```
sbt assembly

java -Dport=8080 \
-Dbatch=128 \
-Ddim=100 \
-Dsample=10000 \
-DnpyFile=./model/10000-100.npy \
-Dtimeout=10000 \
-DtakeSpinCountDelay=5 \
-Xmx8G \
-Xms8G \
-jar ./target/scala-2.12/flasma.jar

```
api
----------------------------------------------------------------
```
curl -X POST http://192.168.0.21:8080/cos -H "Content-Type: application/json"  -d '{"embedding":[0.0,1.1,2.2,3.3,4.4,5.5,6.6,7.7,8.8,9.9,10.10,11.11,12.12,13.13,14.14,15.15,16.16,17.17,18.18,19.19,20.20,21.21,22.22,23.23,24.24,25.25,26.26,27.27,28.28,29.29,30.30,31.31,32.32,33.33,34.34,35.35,36.36,37.37,38.38,39.39,40.40,41.41,42.42,43.43,44.44,45.45,46.46,47.47,48.48,49.49,50.50,51.51,52.52,53.53,54.54,55.55,56.56,57.57,58.58,59.59,60.60,61.61,62.62,63.63,64.64,65.65,66.66,67.67,68.68,69.69,70.70,71.71,72.72,73.73,74.74,75.75,76.76,77.77,78.78,79.79,80.80,81.81,82.82,83.83,84.84,85.85,86.86,87.87,88.88,89.89,90.90,91.91,92.92,93.93,94.94,95.95,96.96,97.97,98.98,99.99]}'
```
```
{"result":[[33609,0.8780051],[9095,0.8760668],[165,0.8757745],[94124,0.8753646], ... ]}

{"result":[[index,score], ... ]}
```

Npy file
----------------------------------------------------------------
```
import numpy as np

item = 100000
dim = 100

a = np.random.rand(item, dim)
b = np.linalg.norm(a , axis=1, keepdims=True)
c = a / b
c = np.transpose(c).astype(np.float32).flatten()

print(c) #[0.07308075 0.00123816 0.17119586 ... 0.08993698 0.1488913  0.07554862
print(c.shape) #(1000000,)
print(c.dtype) #float32

np.save(f"./{item}-{dim}",c)
```

np.save(f"./{item}-{dim}",c)
```
