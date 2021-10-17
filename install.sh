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

download_url="https://github.com/alekcz/pcp/releases/download/$latest_release/pcp-$latest_release-$platform-amd64.zip"

cd "$download_dir"
echo -e "Downloading $download_url."
curl -o "pcp-$latest_release-$platform-amd64.zip" -sL "https://github.com/alekcz/pcp/releases/download/$latest_release/pcp-$latest_release-$platform-amd64.zip"
unzip -qqo "pcp-$latest_release-$platform-amd64.zip"
rm "pcp-$latest_release-$platform-amd64.zip"
mkdir -p "/usr/local/etc/pcp-db"
mkdir -p "$install_dir/pcp-templates"

cd "$install_dir"
if [ -f pcp ]; then
    pcp service stop
    mv -f "$install_dir/pcp" "$install_dir/pcp.old"
    mv -f "$install_dir/pcp-server.jar" "$install_dir/pcp-server.old.jar"
    echo "Moving $install_dir/pcp to $install_dir/pcp.old"
    echo "Moving $install_dir/pcp-server.jar to $install_dir/pcp-server.old.jar"
fi

mv -f "$download_dir/pcp" "$PWD/pcp"

mv -f "$download_dir/pcp-server.jar" "$PWD/pcp-server.jar"

rm -rf "$PWD/pcp-templates"
mv -f "$download_dir/pcp-templates" "$PWD"
printf "(require '[pcp :as pcp])\n(pcp/response 200 \"pew pew\" \"text/plain\")" >> "$PWD/pcp-templates/hello.clj"

case "$(uname -s)" in
    Linux*)     
        mv -f "$download_dir/pcp.service" "/etc/systemd/system/pcp.service"
        mv -f "$download_dir/pcp.service" "/etc/systemd/system/pcp-dev.service"
        systemctl enable pcp.service
        systemctl enable pcp-dev.service
        systemctl start pcp.service
        #systemctl stop pcp.service
        ;;
    Darwin*)    
        mv -f "$download_dir/com.alekcz.pcp.plist" ~/Library/LaunchAgents/com.alekcz.pcp.plist
        mv -f "$download_dir/com.alekcz.pcp.plist" ~/Library/LaunchAgents/com.alekcz.pcp-dev.plist
        chown "$(id -un):$(id -g)"  ~/Library/LaunchAgents/com.alekcz.pcp.plist
        chown "$(id -un):$(id -g)"  ~/Library/LaunchAgents/com.alekcz.pcp-dev.plist
        launchctl load -w ~/Library/LaunchAgents/com.alekcz.pcp.plist
        launchctl load -w ~/Library/LaunchAgents/com.alekcz.pcp-dev.plist
        #launchctl unload  ~/Library/LaunchAgents/com.alekcz.pcp.plist
        ;;
esac

chmod a+x "$PWD/pcp"
pcp service start
echo "Successfully installed pcp in $install_dir."
