package repositories

import (
	"database/sql"
	"fmt"

	lru "github.com/hashicorp/golang-lru"
	"github.com/pkg/errors"

	e "repodiff/entities"
	repoSQL "repodiff/persistence/sql"
)

var cacheSingleton *lru.Cache

const cacheSize = 1024

type source struct {
	db *sql.DB
}

func (s source) getOrCreateURLBranchID(url, branch string) (int16, error) {
	url = protocolStrippedURL(url)
	id, ok := cacheSingleton.Get(cacheKey(url, branch))
	if ok {
		return id.(int16), nil
	}
	val, err := s.getOrCreateURLBranchIDPersistence(url, branch)
	if err != nil {
		return 0, err
	}
	cacheSingleton.Add(cacheKey(url, branch), val)
	return val, nil
}

func (s source) getOrCreateURLBranchIDPersistence(url, branch string) (int16, error) {
	id, err := s.getIDByURLBranch(url, branch)
	if err == nil {
		return id, nil
	}
	s.insertIgnoreError(url, branch)
	return s.getIDByURLBranch(url, branch)
}

func (s source) insertIgnoreError(url, branch string) {
	repoSQL.SingleTransactionInsert(
		s.db,
		`INSERT INTO id_to_url_branch (
			url,
			branch
		) VALUES (?, ?)`,
		[][]interface{}{
			[]interface{}{
				url,
				branch,
			},
		},
	)
}

func (s source) getIDByURLBranch(url, branch string) (int16, error) {
	var id *int16
	repoSQL.Select(
		s.db,
		func(row *sql.Rows) {
			id = new(int16)
			row.Scan(id)
		},
		"SELECT id FROM id_to_url_branch WHERE url = ? AND branch = ?",
		url,
		branch,
	)
	if id == nil {
		return 0, errors.New(fmt.Sprintf("No ID found for %s %s", url, branch))
	}
	return *id, nil
}

func (s source) GetURLBranchByID(id int16) (string, string, error) {
	urlBranchPair, ok := cacheSingleton.Get(id)
	if ok {
		asSlice := urlBranchPair.([]string)
		return asSlice[0], asSlice[1], nil
	}
	url, branch, err := s.getURLBranchByIDPersistence(id)
	if err == nil {
		cacheSingleton.Add(id, []string{url, branch})
	}
	return url, branch, err
}

func (s source) getURLBranchByIDPersistence(id int16) (string, string, error) {
	url := ""
	branch := ""
	repoSQL.Select(
		s.db,
		func(row *sql.Rows) {
			row.Scan(&url, &branch)
		},
		"SELECT url, branch FROM id_to_url_branch WHERE id = ?",
		id,
	)
	if url == "" {
		return "", "", errors.New(fmt.Sprintf("No matching records for ID %d", id))
	}
	return url, branch, nil
}

func (s source) DiffTargetToMapped(target e.DiffTarget) (e.MappedDiffTarget, error) {
	upstream, errU := s.getOrCreateURLBranchID(
		target.Upstream.URL,
		target.Upstream.Branch,
	)
	downstream, errD := s.getOrCreateURLBranchID(
		target.Downstream.URL,
		target.Downstream.Branch,
	)
	if errU != nil || errD != nil {
		return e.MappedDiffTarget{}, errors.New("Failed interacting with the database")
	}
	return e.MappedDiffTarget{
		UpstreamTarget:   upstream,
		DownstreamTarget: downstream,
	}, nil
}

func NewSourceRepository() (source, error) {
	db, err := repoSQL.GetDBConnectionPool()
	return source{
		db: db,
	}, errors.Wrap(err, "Could not establish a database connection")
}

func cacheKey(url, branch string) string {
	return fmt.Sprintf("%s:%s", url, branch)
}

func init() {
	cacheSingleton, _ = lru.New(cacheSize)
}
