.PHONY: clean node_modules dev demo-bundle

clean:
	rm -rf target *.log node_modules

node_modules:
	npm audit && npm install

dev: node_modules
	clojure -A:fig:dev

target/public/cljs-out/prod-main.js: node_modules
	clojure -A:fig:prod

target/demo_bundle/Dockerfile:
	mkdir -p target/demo_bundle
	cp dev-resources/nginx/Dockerfile target/demo_bundle/Dockerfile

target/demo_bundle/nginx.conf:
	mkdir -p target/demo_bundle
	cp dev-resources/nginx/nginx.conf target/demo_bundle/nginx.conf

target/demo_bundle/index.html:
	mkdir -p target/demo_bundle
	cp dev-resources/public/index_prod.html target/demo_bundle/index.html

target/demo_bundle/css:
	mkdir -p target/demo_bundle
	cp -r dev-resources/public/css target/demo_bundle/css

target/demo_bundle/main.js: target/public/cljs-out/prod-main.js
	mkdir -p target/demo_bundle
	cp target/public/cljs-out/prod-main.js target/demo_bundle/main.js

demo-bundle: target/demo_bundle/Dockerfile target/demo_bundle/nginx.conf target/demo_bundle/index.html target/demo_bundle/css target/demo_bundle/main.js
