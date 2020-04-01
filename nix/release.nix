rec {
  cfg = {
    v0_5_0 = rec {
      version = "0.5.0";
      src = {
        url = "https://github.com/eikek/webact/releases/download/v${version}/webact-${version}.zip";
        sha256 = "1a6mcg917l6hzx6xlwkzcxajvkj86iybbdrd65fq7c2yjc3cbvab";
      };
    };
  };
  pkg = v: import ./pkg.nix v;
  currentPkg = pkg cfg.v0_5_0;
  module = ./module.nix;
  modules = [ module
            ];
}
