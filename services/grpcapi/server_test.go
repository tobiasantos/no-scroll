package grpcapi

import (
	"context"
	"net"
	"testing"
	"time"

	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/credentials/insecure"
	"google.golang.org/grpc/status"
	"google.golang.org/grpc/test/bufconn"

	"github.com/tobiasantos/no-scroll/services/evaluator"
	evaluatorv1 "github.com/tobiasantos/no-scroll/services/gen/noscroll/evaluator/v1"
)

func client(t *testing.T) evaluatorv1.EvaluatorServiceClient {
	t.Helper()
	lis := bufconn.Listen(1024 * 1024)
	srv := grpc.NewServer()
	evaluatorv1.RegisterEvaluatorServiceServer(srv, NewServer(evaluator.New(evaluator.DefaultChannels()...)))
	go func() { _ = srv.Serve(lis) }()
	t.Cleanup(srv.Stop)

	conn, err := grpc.NewClient("passthrough:///bufnet",
		grpc.WithContextDialer(func(context.Context, string) (net.Conn, error) { return lis.Dial() }),
		grpc.WithTransportCredentials(insecure.NewCredentials()))
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() { _ = conn.Close() })
	return evaluatorv1.NewEvaluatorServiceClient(conn)
}

func TestEvaluateUsageByGRPC(t *testing.T) {
	resp, err := client(t).EvaluateUsage(context.Background(), &evaluatorv1.EvaluateUsageRequest{AppName: "TikTok", LimitMinutes: 30, SessionMinutes: []int32{25, 10}})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if resp.GetUsedMinutes() != 35 || resp.GetRemainingMinutes() != 0 || !resp.GetExceeded() {
		t.Errorf("expected 35 used, 0 remaining, exceeded; got %v", resp)
	}
	if len(resp.GetDeliveries()) != 3 {
		t.Fatalf("expected 3 deliveries, got %d", len(resp.GetDeliveries()))
	}
	for _, d := range resp.GetDeliveries() {
		if !d.GetDelivered() {
			t.Errorf("channel %s not delivered: %s", d.GetChannel(), d.GetError())
		}
	}
}

func TestInvalidUsageBecomesInvalidArgument(t *testing.T) {
	_, err := client(t).EvaluateUsage(context.Background(), &evaluatorv1.EvaluateUsageRequest{AppName: " ", LimitMinutes: 30})
	if status.Code(err) != codes.InvalidArgument {
		t.Fatalf("expected INVALID_ARGUMENT, got %v", err)
	}
}

func TestCallerDeadlineReachesTheServer(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 300*time.Millisecond)
	defer cancel()

	resp, err := client(t).EvaluateUsage(ctx, &evaluatorv1.EvaluateUsageRequest{AppName: "TikTok", LimitMinutes: 30, SessionMinutes: []int32{40}})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	delivered := map[string]bool{}
	for _, d := range resp.GetDeliveries() {
		delivered[d.GetChannel()] = d.GetDelivered()
	}
	if !delivered["alarm"] || !delivered["push"] || delivered["email"] {
		t.Errorf("expected alarm and push delivered and email not, got %v", delivered)
	}
}
