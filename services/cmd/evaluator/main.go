package main

import (
	"context"
	"log/slog"
	"net"
	"os"
	"os/signal"
	"syscall"
	"time"

	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"
	"google.golang.org/grpc/health"
	healthpb "google.golang.org/grpc/health/grpc_health_v1"
	"google.golang.org/grpc/reflection"

	"github.com/tobiasantos/no-scroll/services/evaluator"
	evaluatorv1 "github.com/tobiasantos/no-scroll/services/gen/noscroll/evaluator/v1"
	"github.com/tobiasantos/no-scroll/services/grpcapi"
)

func main() {
	port := os.Getenv("EVALUATOR_PORT")
	if port == "" {
		port = "50051"
	}
	if len(os.Args) > 1 && os.Args[1] == "health" {
		os.Exit(checkHealth(port))
	}
	lis, err := net.Listen("tcp", ":"+port)
	if err != nil {
		slog.Error("could not listen", "port", port, "error", err)
		os.Exit(1)
	}

	srv := grpc.NewServer()
	evaluatorv1.RegisterEvaluatorServiceServer(srv, grpcapi.NewServer(evaluator.New(evaluator.DefaultChannels()...)))

	healthSrv := health.NewServer()
	healthpb.RegisterHealthServer(srv, healthSrv)
	healthSrv.SetServingStatus(evaluatorv1.EvaluatorService_ServiceDesc.ServiceName, healthpb.HealthCheckResponse_SERVING)

	reflection.Register(srv)

	go func() {
		signals := make(chan os.Signal, 1)
		signal.Notify(signals, syscall.SIGINT, syscall.SIGTERM)
		<-signals
		slog.Info("shutting down")
		healthSrv.Shutdown()
		srv.GracefulStop()
	}()

	slog.Info("evaluator listening", "port", port)
	if err := srv.Serve(lis); err != nil {
		slog.Error("server stopped", "error", err)
		os.Exit(1)
	}
}

func checkHealth(port string) int {
	conn, err := grpc.NewClient("localhost:"+port, grpc.WithTransportCredentials(insecure.NewCredentials()))
	if err != nil {
		return 1
	}
	defer conn.Close()
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Second)
	defer cancel()
	resp, err := healthpb.NewHealthClient(conn).Check(ctx, &healthpb.HealthCheckRequest{})
	if err != nil || resp.GetStatus() != healthpb.HealthCheckResponse_SERVING {
		return 1
	}
	return 0
}
