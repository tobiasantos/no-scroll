package evaluator

import (
	"context"
	"fmt"
	"log/slog"
	"time"
)

type SimulatedChannel struct {
	name  string
	delay time.Duration
}

func NewSimulatedChannel(name string, delay time.Duration) SimulatedChannel {
	return SimulatedChannel{name: name, delay: delay}
}

func (c SimulatedChannel) Name() string { return c.name }

func (c SimulatedChannel) Send(ctx context.Context, alert Alert) error {
	select {
	case <-time.After(c.delay):
		slog.Info("alert sent", "channel", c.name, "app", alert.AppName, "used", alert.UsedMinutes, "limit", alert.LimitMinutes)
		return nil
	case <-ctx.Done():
		return fmt.Errorf("channel %s: %w", c.name, ctx.Err())
	}
}

func DefaultChannels() []Channel {
	return []Channel{
		NewSimulatedChannel("alarm", 50*time.Millisecond),
		NewSimulatedChannel("push", 150*time.Millisecond),
		NewSimulatedChannel("email", 400*time.Millisecond),
	}
}
