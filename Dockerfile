# Dockerfile for BFM Application (using app module's executable JAR)
FROM rockylinux:8

# 0. Disable GPG checks
RUN printf "\n[main]\ngpgcheck=0\nrepo_gpgcheck=0\nskip_if_unavailable=True\n" \
    > /etc/dnf/dnf.conf

# 1. Add Bisoft & EPEL repos
RUN dnf install -y \
      https://nexus.bisoft.com.tr/repository/bfm-yum/repo/bisoft-repo-1.0-1.noarch.rpm \
      https://dl.fedoraproject.org/pub/epel/epel-release-latest-8.noarch.rpm \
    && dnf clean all \
    && rm -rf /var/cache/dnf/*

# 2. Install Java runtime
RUN dnf install -y \
      bfm-rpm-package \
      java-21-openjdk-headless \
    && dnf clean all \
    && rm -rf /var/cache/dnf/*
# 3. Set working directory
WORKDIR /app

# 4. Copy the executable Spring Boot JAR from app module
COPY app/target/bfm-app-3.1.2.jar ./bfm-app.jar

# 5. Copy entrypoint script
COPY entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh

# 6. Expose the application port
EXPOSE 9994

# 7. Launch via entrypoint script
ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]


