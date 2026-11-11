import EquipmentCard from '../cards/EquipmentCard.jsx'

export default function AvailableEquipment({ equipment, onSelect }) {
  return (
    <div className="available-equipment">
      <div className="available-equipment__header">
        <h3>Equipaggiamenti disponibili</h3>
      </div>

      <div className="available-equipment__grid">
        {equipment.map((card) => (
          <EquipmentCard key={card.id} card={card} onSelect={() => onSelect(card)} compact />
        ))}
      </div>
    </div>
  )
}
