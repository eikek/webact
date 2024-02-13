{ pkgs, lib, sbt }:
sbt.lib.mkSbtDerivation {
  inherit pkgs;
  version = "dyn";
  pname = "webact";

  src = lib.sourceByRegex ../. [
    "^build.sbt$"
    "^project$"
    "^project/.*$"
    "^src"
    "^src/.*"
    "elm.json"
  ];

  depsWarmupCommand = ''
    export HOME=$SBT_DEPS/project/home
    mkdir -p $HOME

    # make the webjar contents only
    sbt "make-webapp-only"
    cp target/scala-*/webact_*.jar $HOME/

    # download all deps (sbt update didn't work)
    echo ":quit" | sbt consoleQuick

    # remove garbage
    rm -rf $HOME/.elm
  '';

  nativeBuildInputs = with pkgs; [
    elmPackages.elm
  ];

  depsSha256 = "sha256-YKjsKTbq+0+eoInatFeCKvhdnUD7G5xa13I21Y2jYyc=";
  buildPhase = ''
    export HOME=$(dirname $COURSIER_CACHE)/home
    mkdir lib
    cp -r $HOME/webact_*.jar lib/webact-webjar.jar
    sbt make-without-webapp root/Universal/stage
  '';

  installPhase = ''
    mkdir $out
    cp -R target/universal/stage/* $out/
    mv $out/bin/webact $out/bin/.webact
    cat > $out/bin/webact <<-EOF
    #!${pkgs.bash}/bin/bash
    $out/bin/.webact -java-home ${pkgs.jdk17} "\$@"
    EOF
    chmod 755 $out/bin/webact
  '';
}
