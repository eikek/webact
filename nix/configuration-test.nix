{ modulesPath, config, pkgs, ... }:
{
  imports = [ (modulesPath + "/virtualisation/qemu-vm.nix") ];

  i18n = { defaultLocale = "de_DE.UTF-8"; };
  console.keyMap = "de";

  users.users.root = {
    password = "root";
  };
  users.users.mm = {
    isNormalUser = true;
    password = "mm";
  };
  virtualisation.forwardPorts = [
    {
      from = "host";
      host.port = 64022;
      guest.port = 22;
    }
    {
      from = "host";
      host.port = 64080;
      guest.port = 8011;
    }
  ];

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
    hostName = "webact-test";
    firewall.allowedTCPPorts = [ 8011 ];
  };

  system.stateVersion = "23.11";
}
