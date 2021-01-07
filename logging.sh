#!/usr/bin/env bash

# https://stackoverflow.com/a/2173421/8031185
trap 'trap - SIGTERM && kill -- -$$' SIGINT SIGTERM EXIT

usage() {
    printf "Usage: ./logging.sh [-s SERIAL_NUMBER] [-v V|D|I|W|E|F|S]\n"
    exit 1
}

serials=""
verbosity=""

while :; do
    case $1 in
    -s)
        if [[ -z "${2}" ]]; then
            printf "ERROR: No serial number specified.\n"
            usage
        else
            serials="${2}"
            shift 2
        fi
        ;;
    -v)
        if [[ -z "${2}" ]]; then
            printf "ERROR: No verbosity specified.\n"
            usage
        elif [[ ! ${2} =~ ^[VDIWEFS]$ ]]; then
            printf "ERROR: Invalid ADB logcat verbosity: %s\n" "${2}"
            usage
        else
            verbosity="${2}"
            shift 2
        fi
        ;;
    "")
        break
        ;;
    -?*)
        printf "ERROR: Unknown argument: %s\n" "${1}"
        usage
        ;;
    *)
        printf "ERROR: Unknown argument: %s\n" "${1}"
        usage
        ;;
    esac
done

out_dir="./out/$(date +%Y%m%d_%H%M%S)"
verbose_dir="${out_dir}/verbose/"

if [[ ! -d "${verbose_dir}" ]]; then
    mkdir -p "${verbose_dir}"
fi
if [[ -z ${serials} ]]; then
    # Get `adb.exe devices` output, remove \r and "device", skip first line
    serials=$(tail -n +2 <<<"$(adb.exe devices | sed -r 's/(emulator.*)?(device)?\r$//')")
fi
if [[ -z ${verbosity} ]]; then
    verbosity="W"
fi

for serial in ${serials}; do
    pid="$(adb.exe -s "${serial}" shell ps | awk '/com\.example\.edgesum/ {print $2}')"
    adb.exe -s "${serial}" logcat --pid "${pid}" "*:${verbosity}" >"${out_dir}/${serial}.log" &
    adb.exe -s "${serial}" logcat --pid "${pid}" >"${verbose_dir}/${serial}.log" &
done

wait
