FROM eclipse-temurin:17-jdk-jammy

# Install python 13 and dependencies
RUN apt-get update && apt-get install -y software-properties-common \
    && add-apt-repository ppa:deadsnakes/ppa \
    && apt-get update \
    && apt-get install -y unzip

WORKDIR /alloy-redundancy


# Copy the rest of the application
COPY . .

# Build Alloy and copy the JAR to the analysis results directory
RUN chmod +x ./gradlew
RUN ./gradlew clean build -x test
RUN cp /alloy-redundancy/org.alloytools.alloy.dist/target/org.alloytools.alloy.dist.jar /alloy-redundancy/analysis/results/


# Default command for interactive usage
CMD ["/bin/bash"]