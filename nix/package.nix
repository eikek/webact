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

  # hack for including elm artifacts into the pre-build
  # dependencies; elm stores some stuff in the users home
  # directory. couldn't find official env/config options
  # for the elm compiler - so using $HOME
  depsWarmupCommand = ''
    mkdir -p $SBT_DEPS/project/home
    export HOME=$SBT_DEPS/project/home
    sbt make
    cp -r elm-stuff $HOME/
  '';

  nativeBuildInputs = with pkgs; [
    elmPackages.elm
  ];

  depsSha256 = "sha256-ZiquCQ45ng3/jnekhxoqifTLgUYtajs9hJVVVHTRH/c=";
  buildPhase = ''
    export HOME=$(dirname $COURSIER_CACHE)/home
    cp -r $HOME/elm-stuff .
    sbt make root/Universal/stage
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
