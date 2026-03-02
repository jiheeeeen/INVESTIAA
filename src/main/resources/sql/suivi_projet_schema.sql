CREATE TABLE IF NOT EXISTS projet_suivi (
  id INT AUTO_INCREMENT PRIMARY KEY,
  projet_id INT NOT NULL UNIQUE,
  date_debut_reelle DATE NULL,
  date_fin_cible DATE NULL,
  avancement_global_pct DECIMAL(5,2) NOT NULL DEFAULT 0,
  budget_alloue DECIMAL(15,2) NOT NULL DEFAULT 0,
  budget_consomme DECIMAL(15,2) NOT NULL DEFAULT 0,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_projet_suivi_projet FOREIGN KEY (projet_id) REFERENCES projet(id_projet) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS projet_tache (
  id INT AUTO_INCREMENT PRIMARY KEY,
  projet_id INT NOT NULL,
  titre VARCHAR(180) NOT NULL,
  description TEXT NULL,
  date_tache DATE NOT NULL,
  date_debut DATE NULL,
  date_fin DATE NULL,
  calendar_event_id VARCHAR(191) NULL,
  calendar_status VARCHAR(32) NULL,
  calendar_synced_at TIMESTAMP NULL,
  progression_delta DECIMAL(5,2) NOT NULL DEFAULT 0,
  cout_tache DECIMAL(15,2) NOT NULL DEFAULT 0,
  statut VARCHAR(32) NOT NULL DEFAULT 'TERMINE',
  created_by INT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_projet_tache_projet FOREIGN KEY (projet_id) REFERENCES projet(id_projet) ON DELETE CASCADE
);

ALTER TABLE projet_tache ADD COLUMN IF NOT EXISTS date_debut DATE NULL;
ALTER TABLE projet_tache ADD COLUMN IF NOT EXISTS date_fin DATE NULL;
ALTER TABLE projet_tache ADD COLUMN IF NOT EXISTS calendar_event_id VARCHAR(191) NULL;
ALTER TABLE projet_tache ADD COLUMN IF NOT EXISTS calendar_status VARCHAR(32) NULL;
ALTER TABLE projet_tache ADD COLUMN IF NOT EXISTS calendar_synced_at TIMESTAMP NULL;
UPDATE projet_tache SET date_debut = COALESCE(date_debut, date_tache);
UPDATE projet_tache SET date_fin = COALESCE(date_fin, date_tache);

CREATE TABLE IF NOT EXISTS projet_flux (
  id INT AUTO_INCREMENT PRIMARY KEY,
  projet_id INT NOT NULL,
  type_flux VARCHAR(16) NOT NULL,
  description TEXT NULL,
  montant DECIMAL(15,2) NOT NULL DEFAULT 0,
  date_flux DATE NOT NULL,
  created_by INT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_projet_flux_projet FOREIGN KEY (projet_id) REFERENCES projet(id_projet) ON DELETE CASCADE
);
