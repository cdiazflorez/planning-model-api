FROM hub.furycloud.io/mercadolibre/distroless-java-dev:17-mini

# Setup and install MySQL database for testing
RUN apk add mysql-server

ENV DATABASE_HOST localhost
ENV DATABASE_PASSWORD admin
ENV DATABASE_NAME modellocal

COPY sql/*.sql /mysql-init/
COPY migrations/mysql/modelprod/*.sql /mysql-migrations/
# removes duplicate file
RUN rm /mysql-migrations/20201110185450285_add_dates.sql
RUN rm /mysql-migrations/20210422124346538_create_current_planning_distribution.sql
RUN rm /mysql-migrations/20221013130413744_add_process_path_column_on_headcount_productivity_table.sql
RUN rm /mysql-migrations/20221013132501298_add_process_path_column_on_processing_distribution_table.sql
RUN rm /mysql-migrations/20231018160812091_add_current_processing_distribution_idx_lc_workflow_type_date.sql

# Copy custom commands
ADD ./commands/ /commands/
RUN chmod a+x /commands/*
