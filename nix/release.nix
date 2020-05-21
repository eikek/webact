rec {
  cfg = {
    v0_5_2 = rec {
      version = "0.5.2";
      src = {
        url = "https://github.com/eikek/webact/releases/download/v${version}/webact-${version}.zip";
        sha256 = "1kj0bcsa9gb3qpw8mjflscr4vx0b6m7q1m4vm74mf3iyjc6zb3qf";
      };
    };
    v0_5_1 = rec {
      version = "0.5.1";
      src = {
        url = "https://github.com/eikek/webact/releases/download/v${version}/webact-${version}.zip";
        sha256 = "0dggfdia3xj13ns5w532hvn0hmlfnsvmqg7zbc0hfgzph165pcz6";
      };
    };
    v0_5_0 = rec {
      version = "0.5.0";
      src = {
        url = "https://github.com/eikek/webact/releases/download/v${version}/webact-${version}.zip";
        sha256 = "1a6mcg917l6hzx6xlwkzcxajvkj86iybbdrd65fq7c2yjc3cbvab";
      };
    };
  };
  pkg = v: import ./pkg.nix v;
  currentPkg = pkg cfg.v0_5_2;
  module = ./module.nix;
  modules = [ module
            ];
}
