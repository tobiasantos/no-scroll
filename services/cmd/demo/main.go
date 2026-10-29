package main

import (
	"context"
	"fmt"
	"time"

	"github.com/tobiasantos/no-scroll/services/evaluator"
)

func main() {
	e := evaluator.New(evaluator.DefaultChannels()...)

	result, _ := e.Evaluate(context.Background(), evaluator.Usage{AppName: "Instagram", LimitMinutes: 60, Minutes: []int32{20, 15}})
	fmt.Printf("within limit: used %d, remaining %d, exceeded %v\n", result.UsedMinutes, result.RemainingMinutes, result.Exceeded)

	for _, deadline := range []time.Duration{time.Second, 100 * time.Millisecond} {
		ctx, cancel := context.WithTimeout(context.Background(), deadline)
		start := time.Now()
		result, err := e.Evaluate(ctx, evaluator.Usage{AppName: "TikTok", LimitMinutes: 30, Minutes: []int32{25, 10}})
		cancel()
		if err != nil {
			fmt.Println("error:", err)
			continue
		}
		fmt.Printf("deadline %v, took %v: used %d, exceeded %v\n", deadline, time.Since(start).Round(10*time.Millisecond), result.UsedMinutes, result.Exceeded)
		for _, d := range result.Deliveries {
			if d.Err != nil {
				fmt.Printf("  %-5s not delivered: %v\n", d.Channel, d.Err)
			} else {
				fmt.Printf("  %-5s delivered\n", d.Channel)
			}
		}
	}

	_, err := e.Evaluate(context.Background(), evaluator.Usage{AppName: "", LimitMinutes: 0})
	fmt.Println("empty usage:", err)
}
