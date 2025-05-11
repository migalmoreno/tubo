{
  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs/nixos-unstable";
    systems.url = "github:nix-systems/default";
    flake-parts.url = "github:hercules-ci/flake-parts";
    process-compose-flake.url = "github:Platonic-Systems/process-compose-flake";
    services-flake.url = "github:juspay/services-flake";
  };
  outputs =
    inputs:
    inputs.flake-parts.lib.mkFlake { inherit inputs; } {
      systems = import inputs.systems;
      imports = [ inputs.process-compose-flake.flakeModule ];
      perSystem =
        { config, pkgs, ... }:
        let
          dbName = "tubo";
          dbUser = "tubo";
        in
        {
          devShells.default = pkgs.mkShell {
            inputsFrom = [
              config.process-compose."default".services.outputs.devShell
            ];
            buildInputs = with pkgs; [
              zprint
              clj-kondo
              clojure
              jdk
              nodejs
              pgformatter
            ];
            shellHook = ''
              export PGHOST=/tmp
              export PGDATABASE=${dbName}
              export PGUSER=${dbUser}
            '';
          };
          process-compose."default" =
            { config, ... }:
            {
              imports = [
                inputs.services-flake.processComposeModules.default
              ];
              services.postgres.tubo-db = {
                enable = true;
                socketDir = "/tmp";
                superuser = dbUser;
                initialDatabases = [
                  {
                    name = dbName;
                  }
                ];
              };
              cli.environment.PC_DISABLE_TUI = true;
              cli.options.no-server = false;
              settings.processes.pgweb =
                let
                  pgcfg = config.services.postgres.tubo-db;
                in
                {
                  environment.PGWEB_DATABASE_URL = pgcfg.connectionURI { inherit dbName; };
                  command = pkgs.pgweb;
                  depends_on.tubo-db.condition = "process_healthy";
                };
            };
        };
    };
}
