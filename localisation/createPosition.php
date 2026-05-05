<?php
// Vérifiez bien l'espace après <?php
if ($_SERVER["REQUEST_METHOD"] == "POST") {
    include_once 'service/PositionService.php';
    create();
}

function create() {
    $latitude = $_POST['latitude'] ?? null;
    $longitude = $_POST['longitude'] ?? null;
    $datePosition = $_POST['date_position'] ?? null;
    $imei = $_POST['imei'] ?? 'unknown';

    if ($latitude && $longitude) {
        $service = new PositionService();
        $position = new Position(null, $latitude, $longitude, $datePosition, $imei);
        $service->create($position);
        echo "Position enregistree avec succes";
    } else {
        echo "Donnees manquantes";
    }
}
?>