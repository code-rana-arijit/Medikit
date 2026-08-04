services = {
    "discovery-server": {"port": 8761, "env": ["EUREKA_HOSTNAME=discovery-server", "EUREKA_USERNAME=admin", "EUREKA_PASSWORD=admin"], "replicas": 1, "cpu": "250m", "mem": "512Mi", "db": None},
    "config-server": {"port": 8888, "env": ["CONFIG_USERNAME=config", "CONFIG_PASSWORD=config"], "replicas": 1, "cpu": "250m", "mem": "512Mi", "db": None},
    "api-gateway": {"port": 8080, "env": [], "replicas": 3, "cpu": "500m", "mem": "1Gi", "db": None, "secrets": ["JWT_SECRET"]},
    "user-service": {"port": 8101, "env": [], "replicas": 3, "cpu": "500m", "mem": "1Gi", "db": "user-service-db-url", "secrets": ["JWT_SECRET"]},
    "product-service": {"port": 8102, "env": [], "replicas": 3, "cpu": "500m", "mem": "1Gi", "db": "product-service-db-url"},
    "inventory-service": {"port": 8103, "env": [], "replicas": 3, "cpu": "500m", "mem": "1Gi", "db": "inventory-service-db-url"},
    "cart-service": {"port": 8104, "env": [], "replicas": 3, "cpu": "300m", "mem": "768Mi", "db": None},
    "order-service": {"port": 8105, "env": [], "replicas": 3, "cpu": "500m", "mem": "1Gi", "db": "order-service-db-url"},
    "payment-service": {"port": 8106, "env": [], "replicas": 2, "cpu": "300m", "mem": "768Mi", "db": "payment-service-db-url"},
    "delivery-service": {"port": 8107, "env": [], "replicas": 2, "cpu": "300m", "mem": "768Mi", "db": "delivery-service-db-url"},
    "notification-service": {"port": 8108, "env": [], "replicas": 2, "cpu": "300m", "mem": "768Mi", "db": None},
    "prescription-service": {"port": 8109, "env": [], "replicas": 2, "cpu": "300m", "mem": "768Mi", "db": "prescription-service-db-url"},
    "search-service": {"port": 8110, "env": [], "replicas": 2, "cpu": "300m", "mem": "768Mi", "db": None},
}

def indent(level):
    return "  " * level

for svc, cfg in services.items():
    port = cfg["port"]
    env = ["SPRING_PROFILES_ACTIVE=kubernetes"] + cfg.get("env", [])
    env_yaml = ""
    for e in env:
        k, v = e.split("=", 1)
        env_yaml += f'{indent(6)}- name: {k}\n{indent(7)}value: "{v}"\n'
    common_refs = ["EUREKA_SERVER", "REDIS_HOST", "REDIS_PORT", "KAFKA_BOOTSTRAP_SERVERS"]
    if cfg.get("db"):
        common_refs += ["DB_USERNAME", "DB_PASSWORD"]
    for ref in common_refs:
        env_yaml += f'{indent(6)}- name: {ref}\n{indent(7)}valueFrom:\n{indent(8)}configMapKeyRef:\n{indent(9)}name: medikit-common-config\n{indent(9)}key: {ref}\n'
    if cfg.get("db"):
        env_yaml += f'{indent(6)}- name: DB_URL\n{indent(7)}valueFrom:\n{indent(8)}configMapKeyRef:\n{indent(9)}name: medikit-db-config\n{indent(9)}key: {cfg["db"]}\n'
    for s in cfg.get("secrets", []):
        env_yaml += f'{indent(6)}- name: {s}\n{indent(7)}valueFrom:\n{indent(8)}secretKeyRef:\n{indent(9)}name: medikit-secrets\n{indent(9)}key: {s}\n'

    content = f"""apiVersion: apps/v1
kind: Deployment
metadata:
  name: {svc}
  namespace: medikit
  labels:
    app: {svc}
spec:
  replicas: {cfg['replicas']}
  selector:
    matchLabels:
      app: {svc}
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 1
      maxSurge: 1
  template:
    metadata:
      labels:
        app: {svc}
    spec:
      terminationGracePeriodSeconds: 30
      containers:
        - name: {svc}
          image: ghcr.io/code-rana-arijit/medikit/{svc}:latest
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: {port}
          env:
{env_yaml}{indent(5)}resources:
{indent(6)}requests:
{indent(7)}cpu: {cfg['cpu']}
{indent(7)}memory: {cfg['mem']}
{indent(6)}limits:
{indent(7)}cpu: "2"
{indent(7)}memory: 2Gi
{indent(5)}readinessProbe:
{indent(6)}httpGet:
{indent(7)}path: /actuator/health
{indent(7)}port: {port}
{indent(6)}initialDelaySeconds: 30
{indent(6)}periodSeconds: 10
{indent(6)}timeoutSeconds: 3
{indent(6)}failureThreshold: 5
{indent(5)}livenessProbe:
{indent(6)}httpGet:
{indent(7)}path: /actuator/health
{indent(7)}port: {port}
{indent(6)}initialDelaySeconds: 60
{indent(6)}periodSeconds: 15
{indent(6)}timeoutSeconds: 3
{indent(6)}failureThreshold: 5
---
apiVersion: v1
kind: Service
metadata:
  name: {svc}
  namespace: medikit
  labels:
    app: {svc}
spec:
  selector:
    app: {svc}
  ports:
    - port: {port}
      targetPort: {port}
      name: http
"""
    with open(f"{svc}.yaml", "w") as f:
        f.write(content)
    print(f"generated {svc}.yaml")
