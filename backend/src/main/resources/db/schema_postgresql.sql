CREATE TABLE IF NOT EXISTS scenario (
    id                           uuid             PRIMARY KEY,
    name                         varchar(150)     NOT NULL,
    description                  text,
    created_at                   timestamptz      NOT NULL DEFAULT now(),
    horizon_days                 integer          NOT NULL,
    seed                         bigint,
    stochastic                   boolean          NOT NULL DEFAULT false,

    population_human             double precision,
    vector_ratio                 double precision,
    biting_rate                  double precision,
    beta_human                   double precision,
    beta_vector                  double precision,
    incubation_human_days        double precision,
    incubation_vector_days       double precision,
    infectious_human_days        double precision,
    life_expectancy_days_human   double precision,
    mosquito_lifespan_days       double precision,
    initial_infected_humans      double precision,
    initial_immune_fraction      double precision,

    initial_beds                 double precision,
    hospitalized_fraction        double precision,
    admission_delay_days         double precision,
    avg_stay_days                double precision,
    deterioration_threshold_days double precision,
    grave_mortality_per_day      double precision,
    max_wait_days                double precision,
    severe_fraction              double precision,

    CONSTRAINT scenario_horizon_days_check CHECK (horizon_days > 0 AND horizon_days <= 3650)
);

CREATE TABLE IF NOT EXISTS intervention (
    id           uuid             PRIMARY KEY,
    scenario_id  uuid             NOT NULL,
    type         varchar(30)      NOT NULL,
    start_day    integer          NOT NULL,
    magnitude    double precision NOT NULL,

    CONSTRAINT fk_intervention_scenario
        FOREIGN KEY (scenario_id) REFERENCES scenario (id) ON DELETE CASCADE,
    CONSTRAINT intervention_type_check
        CHECK (type IN ('FUMIGATION', 'EXTRA_BEDS', 'TRANSMISSION_REDUCTION')),
    CONSTRAINT intervention_start_day_check CHECK (start_day >= 0)
);

CREATE INDEX IF NOT EXISTS idx_intervention_scenario ON intervention (scenario_id);

CREATE TABLE IF NOT EXISTS simulation_run (
    id                     uuid             PRIMARY KEY,
    scenario_id            uuid,
    created_at             timestamptz      NOT NULL DEFAULT now(),
    seed                   bigint,
    stochastic             boolean          NOT NULL,

    r0                     double precision,
    saturation_day         integer,
    saturated              boolean,
    peak_pressure_ratio    double precision,
    peak_pressure_day      integer,
    max_bed_deficit        double precision,
    patients_never_got_bed double precision,
    deaths_from_saturation double precision,
    avg_wait_days          double precision,
    final_attack_rate      double precision,
    total_hospitalizations double precision,

    CONSTRAINT fk_simulation_run_scenario
        FOREIGN KEY (scenario_id) REFERENCES scenario (id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_simulation_run_scenario ON simulation_run (scenario_id);
CREATE INDEX IF NOT EXISTS idx_simulation_run_created  ON simulation_run (created_at DESC);
