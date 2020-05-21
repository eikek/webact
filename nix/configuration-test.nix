{ config, pkgs, ... }:
let
  webact = import ./release.nix;
in
{
  imports = webact.modules;

  console.keyMap = "neo";
  i18n = {
    defaultLocale = "en_US.UTF-8";
  };

  users.users.root = {
    password = "root";
  };
  users.users.mm = {
    password = "mm";
    isNormalUser = true;
  };

  nixpkgs = {
    config = {
      packageOverrides = pkgs:
        let
          callPackage = pkgs.lib.callPackageWith(custom // pkgs);
          custom = {
            webact = callPackage webact.currentPkg {};
          };
        in custom;
    };
  };

  services.webact = {
    enable = true;
    userService = true;
    bind.address = "0.0.0.0";
    extra-packages = [
      pkgs.ammonite
    ];
    env = {
      DEBUG = "1";
    };
  };

  services.xserver = {
    enable = false;
  };

  networking = {
    hostName = "webacttest";
    firewall.allowedTCPPorts = [ 8011 ];
  };

  system.stateVersion = "20.03";

}
