{
  description = "Flake for webact";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixpkgs-unstable";
    flake-utils.url = "github:numtide/flake-utils";
    sbt.url = "github:zaninime/sbt-derivation";
  };

  outputs = inputs@{ nixpkgs, flake-utils, sbt, self, ... }:
    {
      overlays.default = final: prev: {
        webact = import ./nix/package.nix {
          inherit (final) pkgs;
          inherit sbt;
          lib = final.pkgs.lib;
        };
      };

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
    } // flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs { inherit system; overlays = [ self.overlays.default ]; };
      in
        rec {
          packages = {
            inherit (pkgs) webact;
            default = self.packages."${system}".webact;
          };

          formatter = pkgs.nixpkgs-fmt;

          apps.default = {
            type = "app";
            program = "${packages.default}/bin/webact";
          };

                  devShells.default =
          let
            run-jekyll = pkgs.writeScriptBin "jekyll-sharry" ''
              jekyll serve -s modules/microsite/target/site --baseurl /sharry
            '';
          in
          pkgs.mkShell {
            buildInputs = with pkgs; [
              pkgs.sbt
              elmPackages.elm

              # for debian packages
              dpkg
              fakeroot
            ];
          };
        });
}
