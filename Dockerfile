FROM eclipse-temurin:17-jdk-jammy

WORKDIR /alloy-redundancy
COPY . /alloy-redundancy/

RUN ./gradlew clean build -x test

RUN chmod +x /alloy-redundancy/analysis/scripts/*

# Extract dataset and rename
RUN tar -xzf /alloy-redundancy/analysis/dataset_2026.tar.gz \
    && mv /alloy-redundancy/dataset_2026 /alloy-redundancy/analysis/dataset \
    && rm /alloy-redundancy/analysis/dataset_2026.tar.gz

# Default command for interactive usage
CMD ["/bin/bash"]