{ lib, stdenv, fetchzip, jdk17, unzip, bash }:
let
  meta = (import ./meta.nix) lib;
  version = meta.latest-release;
in
stdenv.mkDerivation {
  name = "webact-${version}";

  src = fetchzip {
    url = "https://github.com/eikek/webact/releases/download/v${version}/webact-${version}.zip";
    sha256 = "1kj0bcsa9gb3qpw8mjflscr4vx0b6m7q1m4vm74mf3iyjc6zb3qf";
  };

  buildPhase = "true";

  installPhase = ''
    mkdir -p $out/{bin,webact-${version}}
    cp -R * $out/webact-${version}/
    cat > $out/bin/webact <<-EOF
    #!${bash}/bin/bash
    $out/webact-${version}/bin/webact -java-home ${jdk17} "\$@"
    EOF
    chmod 755 $out/bin/webact
  '';

  meta = meta.meta-bin;
}
