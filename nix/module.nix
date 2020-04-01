{config, lib, pkgs, ...}:

with lib;
let
  cfg = config.services.webact;
  user = if cfg.runAs == null then "webact" else cfg.runAs;
  str = e: if (builtins.typeOf e) == "bool" then (if e then "true" else "false") else (builtins.toString e);
  webactConf =
    let
      paths = with builtins;
        (concatMap (p: ["${p}/bin" "${p}/sbin" ]) cfg.extra-packages) ++ (cfg.extra-path);
      converted =
        cfg // { extra-path = paths; };
    in
      pkgs.writeText "webact.conf" ''
        {"webact":
            ${builtins.toJSON converted}
        }
      '';
  defaults = {
    app-name = "Webact";
    script-dir = "/var/lib/webact/scripts";
    tmp-dir = "/var/lib/webact/temp";
    inherit-path = true;
    extra-path = [ "/bin" "/usr/bin" "/run/current-system/sw/bin" ];
    env = {};
    monitor-scripts = true;
    heap-size = "100m";
    bind = {
      address = "localhost";
      port = 8011;
    };
    smtp = {
      host = "";
      port = 0;
      user = "";
      password = "";
      start-tls = false;
      use-ssl = false;
      sender = "noreply@localhost";
    };
  };
in {

  ## interface
  options = {
    services.webact = {
      enable = mkOption {
        default = false;
        description = "Whether to enable webact.";
      };
      runAs = mkOption {
        type = types.nullOr types.str;
        default = null;
        description = ''
          Specify a user for running the application. If null, a new
          user is created.
        '';
      };
      userService = mkOption {
        type = types.bool;
        default = true;
        description = ''
          Whether to run webact as a systemd user service. If
          `false' webact is run as a system service.
        '';
      };

      heap-size = mkOption {
        type = types.str;
        default = defaults.heap-size;
        description = "The maximum heap size for the jvm.";
      };

      app-name = mkOption {
        type = types.str;
        default = defaults.app-name;
        description = "This is shown in the top right corner of the web application";
      };

      script-dir = mkOption {
        type = types.str;
        default =
          if cfg.userService then
            ".webact/scripts"
          else
            defaults.script-dir;
        description = "The directory containing the scripts.";
      };

      tmp-dir = mkOption {
        type = types.str;
        default =
          if cfg.userService then
            ".webact/temp"
          else
            defaults.tmp-dir;
        description = "The directory for storing temporary data.";
      };

      inherit-path = mkOption {
        type = types.bool;
        default = defaults.inherit-path;
        description = "Whether to inherit the PATH env from the server process.";
      };

      extra-path = mkOption {
        type = types.listOf types.str;
        default = defaults.extra-path;
        description = "Directories to append to PATH when the script is run.";
      };

      extra-packages = mkOption {
        type = types.listOf types.package;
        default = [];
        description = ''
          A list of packages whose bin/ and sbin/ directory are added
          to the PATH variable available in scripts.

          This and `extraPaths` are concatenated.
        '';
      };

      env = mkOption {
        type = types.attrsOf types.str;
        default = defaults.env;
        description = "Additional environment variables for a script run.";
      };

      monitor-scripts = mkOption {
        type = types.bool;
        default = defaults.monitor-scripts;
        description = "Whether to monitor the scripts directory for external changes.";
      };

      bind = mkOption {
        type = types.submodule({
          options = {
            address = mkOption {
              type = types.str;
              default = defaults.bind.address;
              description = "The address to bind the REST server to.";
            };
            port = mkOption {
              type = types.int;
              default = defaults.bind.port;
              description = "The port to bind the REST server";
            };
          };
        });
        default = defaults.bind;
        description = "Address and port bind the rest server.";
      };

      smtp = mkOption {
        type = types.submodule({
          options = {
            host = mkOption {
              type = types.str;
              default = defaults.backend.mail.smtp.host;
              description = "Host or IP of the SMTP server.";
            };
            port = mkOption {
              type = types.int;
              default = defaults.backend.mail.smtp.port;
              description = "Port of the SMTP server.";
            };
            user = mkOption {
              type = types.str;
              default = defaults.backend.mail.smtp.user;
              description = ''
                User to authenticate at the server. If the user
                is empty, mails are sent without authentication.
              '';
            };
            password = mkOption {
              type = types.str;
              default = defaults.backend.mail.smtp.password;
              description = "Password for authentication at the server.";
            };
            start-tls = mkOption {
              type = types.bool;
              default = defaults.smtp.start-tls;
              description = "Whether to use StartTLS";
            };
            use-ssl = mkOption {
              type = types.bool;
              default = defaults.smtp.start-tls;
              description = "Whether to use SSL";
            };
            sender = mkOption {
              type = types.str;
              default = defaults.smtp.sender;
              description = "The sender address to use.";
            };
          };
        });
        default = defaults.smtp;
        description = "SMTP Settings";
      };
    };
  };

  ## implementation
  config = mkIf config.services.webact.enable {

    users.users."${user}" = mkIf (cfg.runAs == null && !cfg.userService) {
      name = user;
      isSystemUser = true;
      description = "Webact user";
    };

    systemd.user.services.webact = mkIf config.services.webact.userService {
      description = "Webact User Service";
      wantedBy = [ "default.target" ];
      restartIfChanged = true;
      serviceConfig = {
        RestartSec = 3;
        Restart = "always";
      };
      path = [ pkgs.gawk ];
      preStart = ''
        mkdir -p ${cfg.script-dir}
        mkdir -p ${cfg.tmp-dir}
      '';

      script = "${pkgs.webact}/bin/webact -J-Xmx${cfg.heap-size} ${webactConf}";
    };

    systemd.services.webact =
      let
        cmd = "${pkgs.webact}/bin/webact -J-Xmx${cfg.heap-size} ${webactConf}";
      in
        mkIf (!cfg.userService) {
          description = "Webact Server";
          after = [ "networking.target" ];
          wantedBy = [ "multi-user.target" ];
          path = [ pkgs.gawk ];
          preStart = ''
            mkdir -p ${cfg.script-dir}
            mkdir -p ${cfg.tmp-dir}
            chown ${user} ${cfg.script-dir} ${cfg.tmp-dir}
          '';
          script =
            "${pkgs.su}/bin/su -s ${pkgs.bash}/bin/sh ${user} -c \"${cmd}\"";
        };
  };
}
