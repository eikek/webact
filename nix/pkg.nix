cfg: { stdenv, lib, fetchzip, jre8_headless, unzip, bash }:

stdenv.mkDerivation rec {
  name = "webact-${cfg.version}";

  src = fetchzip cfg.src;

  buildPhase = "true";

  installPhase = ''
    mkdir -p $out/{bin,webact-${cfg.version}}
    cp -R * $out/webact-${cfg.version}/
    cat > $out/bin/webact <<-EOF
    #!${bash}/bin/bash
    $out/webact-${cfg.version}/bin/webact -java-home ${jre8_headless} "\$@"
    EOF
    chmod 755 $out/bin/webact
  '';

  meta = {
    description = "Run actions from the web. Webact allows to manage scripts on the server and execute them.";
    license = lib.licenses.gpl3;
    homepage = https://github.com/eikek/webact;
  };
}
