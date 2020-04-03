#!/usr/bin/env bash

set -euo pipefail

print_help() {
    echo "Installs latest version of pcp. Installation directory defaults to /usr/local/bin."
    echo -e
    echo "Usage:"
    echo "installer.sh [<dir>]"
    exit 1
}

default_install_dir="/usr/local/bin"
install_dir=$default_install_dir
install_dir_opt=${1:-}
if [ "$install_dir_opt" ]; then
    install_dir="$install_dir_opt"
fi

download_dir=/tmp

latest_release="$(curl -sL https://raw.githubusercontent.com/alekcz/pcp/master/resources/PCP_RELEASED_VERSION)"

case "$(uname -s)" in
    Linux*)     platform=linux;;
    Darwin*)    platform=macos;;
esac

download_url="https://github.com/alekcz/pcp/releases/download/v$latest_release/pcp-v$latest_release-$platform-amd64.zip"

cd "$download_dir"
echo -e "Downloading $download_url."
curl -o "pcp-$latest_release-$platform-amd64.zip" -sL "https://github.com/alekcz/pcp/releases/download/v$latest_release/pcp-v$latest_release-$platform-amd64.zip"
unzip -qqo "pcp-$latest_release-$platform-amd64.zip"
rm "pcp-$latest_release-$platform-amd64.zip"

cd "$install_dir"
if [ -f pcp ]; then
    echo "Moving $install_dir/pcp to $install_dir/pcp.old"
fi

mv -f "$download_dir/pcp" "$PWD/pcp"

mv -f "$download_dir/pcp.jar" "$PWD/pcp.jar"

mv -f "$download_dir/pcp.service" "/etc/systemd/system/pcp.service"

systemctl enable pcp.service

systemctl start pcp.service

chmod a+x "$PWD/pcp"

echo "Successfully installed pcp in $install_dir."