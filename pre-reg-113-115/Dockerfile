FROM openjdk:11

# can be passed during Docker build as build time environment variable for mosip user level change.
ARG container_user=mosip
ARG container_user_group=mosip
ARG container_user_uid=1001
ARG container_user_gid=1001

# can be passed during Docker build as build time environment for label related addition to docker.
ARG SOURCE
ARG COMMIT_HASH
ARG COMMIT_ID
ARG BUILD_TIME

# can be passed during Docker build as build time environment for label.
LABEL source=${SOURCE}
LABEL commit_hash=${COMMIT_HASH}
LABEL commit_id=${COMMIT_ID}
LABEL build_time=${BUILD_TIME}

# Set the working directory
ENV work_dir=/

# Create directory for additional JARs
ARG loader_path=${work_dir}/additional_jars/

# Update, install dependencies, Create the user and group
RUN apt-get update \
&& apt-get -y upgrade \
&& apt-get -y install unzip \
&& groupadd -g ${container_user_gid} ${container_user_group} \
&& useradd -u ${container_user_uid} -g ${container_user_gid} -m -s /bin/bash ${container_user} \
&& chown -R ${container_user}:${container_user} /home/${container_user} \
&& mkdir -p ${loader_path}

# Set environment variable for loader path
ENV loader_path_env=${loader_path}

# Copy the JAR file
ADD target/ReEncryptUtility-*.jar ReEncryptUtility.jar

# Set the working directory
WORKDIR /home/${container_user}

# Switch to the specified user
USER ${container_user_uid}:${container_user_gid}

# Define the command to run the application
CMD ["java","-jar", "-Dloader.path=${loader_path_env}", "-Dspring.cloud.config.label=${spring_config_label_env}","-Dspring.cloud.config.name=${spring_config_name_env}", "-Dspring.profiles.active=${active_profile_env}", "-Dspring.cloud.config.uri=${spring_config_url_env}", "/ReEncryptUtility.jar"]
