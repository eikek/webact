{
  description = "Flake for webact";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-23.11";
    sbt.url = "github:zaninime/sbt-derivation";
  };

  outputs = inputs@{ nixpkgs, flake-parts, sbt, self, ... }:
    flake-parts.lib.mkFlake { inherit inputs; } {
      imports = [ inputs.flake-parts.flakeModules.easyOverlay ];
      systems = [ "x86_64-linux" "x86_64-darwin" ];

      perSystem = { config, pkgs, lib, ... }:
        rec {
          packages = rec {
            default = webact;
            webact = sbt.lib.mkSbtDerivation {
              inherit pkgs;
              version = "dyn";
              pname = "webact";

              src = lib.sourceByRegex ./. [
                "^build.sbt$"
                "^project$"
                "^project/.*$"
                "^src"
                "^src/.*"
                "elm.json"
              ];

              # hack for including elm artifacts into the
              # dependencies; couldn't find official env/config
              # options for the elm compiler - so using $HOME
              depsWarmupCommand = ''
                mkdir -p $SBT_DEPS/project/.ivy/home
                export HOME=$SBT_DEPS/project/.ivy/home
                sbt make
                cp -r elm-stuff $HOME/
              '';

              nativeBuildInputs = with pkgs; [
                elmPackages.elm
              ];

              depsSha256 = "sha256-wf5ItlmSqFq6bo5VMXfOKD9DckC4tJ8upbJVQ14xDWQ=";
              buildPhase = ''
                export HOME=$(dirname $COURSIER_CACHE)/.ivy/home
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
            };
          };
          apps.default = {
            type = "app";
            program = "${packages.default}/bin/webact";
          };
          overlayAttrs = { inherit (config.packages) webact; };
          formatter = pkgs.nixpkgs-fmt;
        };

      flake = {
        nixosModules = rec {
          webact = import ./nix/module.nix;
          default = webact;
        };

        nixosConfigurations.test-vm =
          let
            system = "x86_64-linux";
            pkgs = import inputs.nixpkgs {
              inherit system;
              overlays = [ self.overlays.default ];
            };

          in
          nixpkgs.lib.nixosSystem {
            inherit pkgs system;
            specialArgs = inputs;
            modules = [
              self.nixosModules.webact
              ./nix/configuration-test.nix
            ];
          };
      };
    };
}
