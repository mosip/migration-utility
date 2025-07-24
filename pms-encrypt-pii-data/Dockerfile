FROM eclipse-temurin:21-jre-alpine

ARG SOURCE
ARG COMMIT_HASH
ARG COMMIT_ID
ARG BUILD_TIME
LABEL source=${SOURCE}
LABEL commit_hash=${COMMIT_HASH}
LABEL commit_id=${COMMIT_ID}
LABEL build_time=${BUILD_TIME}

# Build-time arguments for configuration
ARG spring_config_label
ARG active_profile
ARG spring_config_url

# Runtime environment variables
ENV active_profile_env=${active_profile}
ENV spring_config_label_env=${spring_config_label}
ENV spring_config_url_env=${spring_config_url}

ARG container_user=mosip
ARG container_user_group=mosip
ARG container_user_uid=1002
ARG container_user_gid=1001

# Install packages and create user
RUN apk --no-cache add unzip wget \
 && addgroup -g ${container_user_gid} ${container_user_group} \
 && adduser -s /bin/sh -u ${container_user_uid} -G ${container_user_group} -h /home/${container_user} --disabled-password ${container_user}

WORKDIR /home/${container_user}

# Copy the built JAR file (update the name as per your artifact)
COPY ./target/partner-pii-data-encrypt-utility-*.jar partner-pii-data-encrypt-utility.jar

RUN chown -R ${container_user}:${container_user} /home/${container_user}

USER ${container_user_uid}:${container_user_gid}

EXPOSE 9103

CMD java -XX:-UseG1GC -XX:-UseParallelGC -XX:-UseShenandoahGC -XX:+ExplicitGCInvokesConcurrent -XX:+UseZGC -XX:+ZGenerational -XX:+UnlockExperimentalVMOptions -XX:+UseStringDeduplication -XX:+HeapDumpOnOutOfMemoryError -XX:+UseCompressedOops -XX:MaxGCPauseMillis=200 -Dfile.encoding=UTF-8 \
    -Dspring.cloud.config.label="${spring_config_label_env}" \
    -Dspring.profiles.active="${active_profile_env}" \
    -Dspring.cloud.config.uri="${spring_config_url_env}" \
    -jar partner-pii-data-encrypt-utility.jar