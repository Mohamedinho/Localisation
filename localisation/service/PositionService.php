<?php
// On définit la racine du projet
$root = dirname(__DIR__);

include_once $root . '/dao/IDao.php';
include_once $root . '/classe/Position.php';
include_once $root . '/connexion/Connexion.php';

class PositionService implements IDao {
    private $connexion;

    public function __construct() {
        $this->connexion = new Connexion();
    }

    public function create($position) {
        // AJOUT DES BACKTICKS ` AU NOM DE LA TABLE
        $sql = "INSERT INTO `position` (latitude, longitude, date_position, imei) 
                VALUES (:latitude, :longitude, :date_position, :imei)";
        
        $stmt = $this->connexion->getConnexion()->prepare($sql);
        $stmt->execute([
            ':latitude' => $position->getLatitude(),
            ':longitude' => $position->getLongitude(),
            ':date_position' => $position->getDatePosition(),
            ':imei' => $position->getImei()
        ]);
    }

    public function update($obj) {}
    public function delete($obj) {}
    public function getById($obj) {}
    public function getAll() {}
}
?>
