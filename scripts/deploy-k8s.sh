#!/usr/bin/env bash
set -euo pipefail

# Deployment helper for Kubernetes
# Prerequisites: kubectl configured for target cluster
# Usage: ./scripts/deploy-k8s.sh [full|rollout-status|logs|destroy]

ACTION="${1:-full}"
NS="medikit"

case "$ACTION" in
  full)
    echo ">> Creating namespace"
    kubectl apply -f k8s/base/namespace.yaml

    echo ">> Applying configmaps and secrets"
    kubectl apply -f k8s/base/config/

    echo ">> Applying infrastructure (postgres, redis, kafka)"
    kubectl apply -f k8s/postgres/postgres.yaml
    kubectl apply -f k8s/redis/redis.yaml
    kubectl apply -f k8s/kafka/kafka.yaml

    echo ">> Applying service deployments"
    for svc in discovery-server config-server api-gateway user-service product-service inventory-service cart-service order-service payment-service delivery-service notification-service prescription-service search-service; do
      kubectl apply -f k8s/deployments/$svc.yaml
    done

    echo ">> Applying autoscaling"
    kubectl apply -f k8s/hpa/

    echo ">> Applying ingress"
    kubectl apply -f k8s/ingress/ingress.yaml

    echo ">> Waiting for rollout..."
    kubectl rollout status deployment -n $NS discovery-server config-server api-gateway --timeout=180s
    kubectl rollout status deployment -n $NS user-service product-service inventory-service cart-service order-service payment-service delivery-service notification-service prescription-service search-service --timeout=300s
    ;;
  rollout-status)
    kubectl rollout status deployment -n $NS --all --timeout=60s
    ;;
  logs)
    kubectl get pods -n $NS
    echo ""
    echo "Follow a service log with: kubectl logs -n $NS deploy/<service> -f"
    ;;
  destroy)
    echo ">> Removing all Medikit resources"
    kubectl delete namespace $NS --ignore-not-found
    ;;
  *)
    echo "Usage: $0 [full|rollout-status|logs|destroy]"
    exit 1
    ;;
esac
