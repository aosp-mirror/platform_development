package interactors

func ExistingErrorOr(existing error, toEvaluate func() error) error {
	if existing != nil {
		return existing
	}
	return toEvaluate()
}

func AnyError(errors ...error) error {
	for _, err := range errors {
		if err != nil {
			return err
		}
	}
	return nil
}
