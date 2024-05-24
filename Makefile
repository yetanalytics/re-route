node_modules:
	npm audit && npm install

################################################################################
# Dev Targets
################################################################################

clean:
	rm -rf target *.log node_modules resources/public/css/style.css resources/public/css/style.css.map

clean-css:
	rm resources/public/css/style.css

dev: node_modules
	clojure -A:dev:build
