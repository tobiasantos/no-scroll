package grpcapi

import (
	"context"
	"errors"
	"time"

	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"

	"github.com/tobiasantos/no-scroll/services/evaluator"
	evaluatorv1 "github.com/tobiasantos/no-scroll/services/gen/noscroll/evaluator/v1"
)

const margin = 50 * time.Millisecond

type Server struct {
	evaluatorv1.UnimplementedEvaluatorServiceServer
	evaluator *evaluator.Evaluator
}

func NewServer(e *evaluator.Evaluator) *Server {
	return &Server{evaluator: e}
}

func (s *Server) EvaluateUsage(ctx context.Context, req *evaluatorv1.EvaluateUsageRequest) (*evaluatorv1.EvaluateUsageResponse, error) {
	if deadline, ok := ctx.Deadline(); ok {
		var cancel context.CancelFunc
		ctx, cancel = context.WithDeadline(ctx, deadline.Add(-margin))
		defer cancel()
	}
	usage := evaluator.Usage{AppName: req.GetAppName(), LimitMinutes: req.GetLimitMinutes(), Minutes: req.GetSessionMinutes()}
	result, err := s.evaluator.Evaluate(ctx, usage)
	if errors.Is(err, evaluator.ErrInvalidUsage) {
		return nil, status.Error(codes.InvalidArgument, err.Error())
	}
	if err != nil {
		return nil, status.Error(codes.Internal, "evaluation failed")
	}

	resp := &evaluatorv1.EvaluateUsageResponse{
		UsedMinutes:      result.UsedMinutes,
		RemainingMinutes: result.RemainingMinutes,
		Exceeded:         result.Exceeded,
	}
	for _, d := range result.Deliveries {
		delivery := &evaluatorv1.Delivery{Channel: d.Channel, Delivered: d.Err == nil}
		if d.Err != nil {
			delivery.Error = d.Err.Error()
		}
		resp.Deliveries = append(resp.Deliveries, delivery)
	}
	return resp, nil
}
