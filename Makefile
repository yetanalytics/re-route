.PHONY: clean node_modules dev bundle

clean:
	rm -rf target *.log node_modules

node_modules:
	npm audit && npm install

dev: node_modules
	clojure -A:fig:dev

target/public/cljs-out/prod-main.js: node_modules
	clojure -A:fig:prod

target/bundle/Dockerfile:
	mkdir -p target/bundle
	cp dev-resources/Dockerfile target/bundle/Dockerfile

target/bundle/nginx.conf:
	mkdir -p target/bundle
	cp dev-resources/nginx.conf target/bundle/nginx.conf

target/bundle/index.html:
	mkdir -p target/bundle
	cp resources/public/index_prod.html target/bundle/index.html

target/bundle/css:
	mkdir -p target/bundle
	cp -r resources/public/css target/bundle/css

target/bundle/main.js: target/public/cljs-out/prod-main.js
	mkdir -p target/bundle
	cp target/public/cljs-out/prod-main.js target/bundle/main.js

bundle: target/bundle/Dockerfile target/bundle/nginx.conf target/bundle/index.html target/bundle/css target/bundle/main.js
