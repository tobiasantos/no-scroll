FROM golang:1.27 AS build
WORKDIR /src
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 go build -o /evaluator ./cmd/evaluator

FROM gcr.io/distroless/static-debian12
COPY --from=build /evaluator /evaluator
USER nonroot
EXPOSE 50051
ENTRYPOINT ["/evaluator"]
