clean:
	rm -rf target *.log node_modules

node_modules:
	npm audit && npm install

dev: node_modules
	clojure -A:dev:build
