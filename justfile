set shell := ["bash", "-euo", "pipefail", "-c"]

default:
    @just --list

list-versions:
    @find versions -mindepth 2 -maxdepth 2 -type f -name 'gradle.properties' -printf '%h\n' | xargs -r -n1 basename | sort -V

list-loaders version:
    @grep '^project.enabled-loaders=' "versions/{{ version }}/gradle.properties" | head -n1 | cut -d= -f2- | tr ',' '\n' | sed 's/^[[:space:]]*//; s/[[:space:]]*$//' | sed '/^$/d'

list-nodes:
    @for props in versions/*/gradle.properties; do version=$(basename "$(dirname "$props")"); loaders=$(sed -nE 's/^project\.enabled-loaders=(.*)$/\1/p' "$props" | head -n1); for loader in $(printf '%s\n' "$loaders" | tr ',' '\n' | sed 's/^[[:space:]]*//; s/[[:space:]]*$//' | sed '/^$/d'); do echo "$version-$loader"; done; done | sort -V

clean-generated:
    @rm -rf build common/versions fabric/versions forge/versions neoforge/versions .teakit

build node:
    @if ! just list-nodes | grep -Fxq "{{ node }}"; then echo "Unknown node: {{ node }}"; exit 1; fi
    @version="{{ node }}"; loader="${version##*-}"; version="${version%-*}"; ./gradlew --configure-on-demand ":$loader:$version:build" --console=plain

build-all:
    @./gradlew build --console=plain

compile-all:
    @tasks=(); for version in $(just list-versions); do tasks+=(":common:$version:compileJava"); for loader in $(just list-loaders "$version"); do tasks+=(":$loader:$version:compileJava"); done; done; ./gradlew --configure-on-demand "${tasks[@]}" --console=plain

run-client node:
    @if ! just list-nodes | grep -Fxq "{{ node }}"; then echo "Unknown node: {{ node }}"; exit 1; fi
    @version="{{ node }}"; loader="${version##*-}"; version="${version%-*}"; ./gradlew --configure-on-demand ":$loader:$version:runClient" --console=plain

teakit-check node test_file="test/teakit/minisort.test.ts" timeout="300":
    @./teakitw run --node "{{ node }}" --test-file "{{ test_file }}" --timeout "{{ timeout }}"

horizontal-jars version="all":
    @versions="{{ version }}"; if [ "$versions" = all ]; then versions=$(just list-versions); elif ! just list-versions | grep -Fxq "$versions"; then echo "Unknown version: $versions"; exit 1; fi; for version in $versions; do echo "==> $version horizontal jar"; ./gradlew -Pmultiloader.target.versions="$version" validateHorizontalJars --console=plain || exit; done

# Runs the dedicated-server pair test against the horizontal jar; build it first with `just horizontal-jars <version>`.
pair node timeout="300":
    @modstage clean instance "{{ node }}" --side server > /dev/null 2>&1 || true
    @env -u WAYLAND_DISPLAY SDL_VIDEO_DRIVER=x11 GLFW_PLATFORM=x11 XDG_SESSION_TYPE=x11 xvfb-run -a -s "-screen 0 1920x1080x24" ./teakitw pair --node "{{ node }}" --modstage-config modstage.toml --modstage-instance "{{ node }}" --server-address 127.0.0.1:25592 --test-file test/teakit-pair/dedicated-server.test.ts --timeout "{{ timeout }}"

publish-version version *args:
    @tasks=(":common:{{ version }}:publishAllPublicationsToKafMavenRepository"); for loader in $(just list-loaders "{{ version }}"); do tasks+=(":$loader:{{ version }}:publishAllPublicationsToKafMavenRepository"); done; ./gradlew --configure-on-demand "${tasks[@]}" {{ args }} --console=plain
