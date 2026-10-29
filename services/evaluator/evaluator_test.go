package evaluator

import (
	"context"
	"errors"
	"testing"
	"time"
)

func TestEvaluateWithinLimitDoesNotAlert(t *testing.T) {
	e := New(DefaultChannels()...)

	result, err := e.Evaluate(context.Background(), Usage{AppName: "Instagram", LimitMinutes: 60, Minutes: []int32{20, 15}})

	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if result.UsedMinutes != 35 || result.RemainingMinutes != 25 || result.Exceeded {
		t.Errorf("expected 35 used, 25 remaining, not exceeded; got %+v", result)
	}
	if len(result.Deliveries) != 0 {
		t.Errorf("expected no alerts, got %d", len(result.Deliveries))
	}
}

func TestEvaluateExceededAlertsAllChannelsAtTheSameTime(t *testing.T) {
	e := New(DefaultChannels()...)

	start := time.Now()
	result, err := e.Evaluate(context.Background(), Usage{AppName: "TikTok", LimitMinutes: 30, Minutes: []int32{25, 10}})
	elapsed := time.Since(start)

	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if result.UsedMinutes != 35 || result.RemainingMinutes != 0 || !result.Exceeded {
		t.Errorf("expected 35 used, 0 remaining, exceeded; got %+v", result)
	}
	if len(result.Deliveries) != 3 {
		t.Fatalf("expected 3 deliveries, got %d", len(result.Deliveries))
	}
	for _, d := range result.Deliveries {
		if d.Err != nil {
			t.Errorf("channel %s failed: %v", d.Channel, d.Err)
		}
	}
	if elapsed > 550*time.Millisecond {
		t.Errorf("took %v: channels did not run at the same time", elapsed)
	}
}

func TestEvaluateRespectsContextDeadline(t *testing.T) {
	e := New(DefaultChannels()...)
	ctx, cancel := context.WithTimeout(context.Background(), 100*time.Millisecond)
	defer cancel()

	result, err := e.Evaluate(ctx, Usage{AppName: "TikTok", LimitMinutes: 30, Minutes: []int32{40}})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	byChannel := map[string]error{}
	for _, d := range result.Deliveries {
		byChannel[d.Channel] = d.Err
	}
	if byChannel["alarm"] != nil {
		t.Errorf("alarm (50 ms) should be delivered before the deadline: %v", byChannel["alarm"])
	}
	for _, slow := range []string{"push", "email"} {
		if !errors.Is(byChannel[slow], context.DeadlineExceeded) {
			t.Errorf("%s should give up on deadline, got %v", slow, byChannel[slow])
		}
	}
}

func TestEvaluateRejectsInvalidUsage(t *testing.T) {
	e := New(DefaultChannels()...)
	cases := []struct {
		name  string
		usage Usage
	}{
		{"blank app", Usage{AppName: "  ", LimitMinutes: 30}},
		{"zero limit", Usage{AppName: "TikTok", LimitMinutes: 0}},
		{"limit over a day", Usage{AppName: "TikTok", LimitMinutes: 1441}},
		{"session without minutes", Usage{AppName: "TikTok", LimitMinutes: 30, Minutes: []int32{10, 0}}},
	}
	for _, c := range cases {
		t.Run(c.name, func(t *testing.T) {
			_, err := e.Evaluate(context.Background(), c.usage)
			if !errors.Is(err, ErrInvalidUsage) {
				t.Errorf("expected ErrInvalidUsage, got %v", err)
			}
		})
	}
}
