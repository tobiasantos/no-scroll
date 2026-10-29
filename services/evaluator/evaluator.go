package evaluator

import (
	"context"
	"errors"
	"fmt"
	"strings"
	"sync"
)

type Usage struct {
	AppName      string
	LimitMinutes int32
	Minutes      []int32
}

type Alert struct {
	AppName      string
	UsedMinutes  int32
	LimitMinutes int32
}

type Channel interface {
	Name() string
	Send(ctx context.Context, alert Alert) error
}

type Delivery struct {
	Channel string
	Err     error
}

type Result struct {
	UsedMinutes      int32
	RemainingMinutes int32
	Exceeded         bool
	Deliveries       []Delivery
}

var ErrInvalidUsage = errors.New("invalid usage")

type Evaluator struct {
	channels []Channel
}

func New(channels ...Channel) *Evaluator {
	return &Evaluator{channels: channels}
}

func (e *Evaluator) Evaluate(ctx context.Context, usage Usage) (Result, error) {
	if err := validate(usage); err != nil {
		return Result{}, err
	}

	var used int32
	for _, minutes := range usage.Minutes {
		used += minutes
	}
	result := Result{
		UsedMinutes:      used,
		RemainingMinutes: max(usage.LimitMinutes-used, 0),
		Exceeded:         used > usage.LimitMinutes,
	}
	if result.Exceeded {
		alert := Alert{AppName: usage.AppName, UsedMinutes: used, LimitMinutes: usage.LimitMinutes}
		result.Deliveries = e.alert(ctx, alert)
	}
	return result, nil
}

func (e *Evaluator) alert(ctx context.Context, alert Alert) []Delivery {
	deliveries := make([]Delivery, len(e.channels))
	var wg sync.WaitGroup
	for i, channel := range e.channels {
		wg.Go(func() {
			deliveries[i] = Delivery{Channel: channel.Name(), Err: channel.Send(ctx, alert)}
		})
	}
	wg.Wait()
	return deliveries
}

func validate(usage Usage) error {
	if strings.TrimSpace(usage.AppName) == "" || usage.LimitMinutes < 1 || usage.LimitMinutes > 1440 {
		return fmt.Errorf("%w: app %q, limit %d", ErrInvalidUsage, usage.AppName, usage.LimitMinutes)
	}
	for _, minutes := range usage.Minutes {
		if minutes <= 0 {
			return fmt.Errorf("%w: session with %d minutes", ErrInvalidUsage, minutes)
		}
	}
	return nil
}
