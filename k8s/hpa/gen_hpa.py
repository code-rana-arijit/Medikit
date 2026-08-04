hpas = {
    "api-gateway": {"min": 3, "max": 20, "cpu": 70},
    "user-service": {"min": 3, "max": 30, "cpu": 70},
    "product-service": {"min": 3, "max": 30, "cpu": 70},
    "inventory-service": {"min": 3, "max": 20, "cpu": 70},
    "cart-service": {"min": 3, "max": 20, "cpu": 70},
    "order-service": {"min": 3, "max": 20, "cpu": 70},
    "payment-service": {"min": 2, "max": 15, "cpu": 70},
    "delivery-service": {"min": 2, "max": 15, "cpu": 70},
    "notification-service": {"min": 2, "max": 10, "cpu": 70},
    "prescription-service": {"min": 2, "max": 10, "cpu": 70},
    "search-service": {"min": 2, "max": 10, "cpu": 70},
}
for svc, cfg in hpas.items():
    content = f"""apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: {svc}-hpa
  namespace: medikit
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: {svc}
  minReplicas: {cfg['min']}
  maxReplicas: {cfg['max']}
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: {cfg['cpu']}
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 75
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
        - type: Percent
          value: 100
          periodSeconds: 30
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
        - type: Percent
          value: 25
          periodSeconds: 60
"""
    with open(f"{svc}-hpa.yaml", "w") as f:
        f.write(content)
    print(f"generated {svc}-hpa.yaml")
