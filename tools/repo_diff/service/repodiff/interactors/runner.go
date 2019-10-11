package interactors

import (
	"sync"

	"github.com/pkg/errors"
)

type taskRunner struct {
	errorChan chan error
	sync.Mutex
	sync.WaitGroup
}

func NewTaskRunner() *taskRunner {
	return &taskRunner{}
}

func (t *taskRunner) ExecuteFunctionsAsync(functions []func() error) error {
	t.Lock()
	defer t.Unlock()
	t.errorChan = make(chan error)
	t.spawnTasksAsync(
		t.syncErrorFnToAsync(functions),
	)
	go t.closeErrorChanOnComplete()
	return t.breakOnError()
}

func (t *taskRunner) breakOnError() error {
	return <-t.errorChan
}

func (t *taskRunner) syncErrorFnToAsync(functions []func() error) []func() {
	transformed := make([]func(), len(functions))
	for i, fn := range functions {
		transformed[i] = t.redirectErrToChannel(fn)
	}
	return transformed
}

func (t *taskRunner) closeErrorChanOnComplete() {
	t.Wait()
	close(t.errorChan)
}

func (t *taskRunner) spawnTasksAsync(tasks []func()) {
	t.Add(len(tasks))
	for _, task := range tasks {
		go task()
	}
}

func (t *taskRunner) redirectErrToChannel(f func() error) func() {
	return func() {
		defer t.Done()
		err := f()
		if err != nil && t.errorChan != nil {
			t.errorChan <- errors.Wrap(err, "Error redirected to channel")
		}
	}
}
