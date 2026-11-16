import CardSlot from '../cards/CardSlot.jsx'

export default function SelectedEquipment({ equipment, onRemove, onAddSlot }) {
  return (
    <div className="selected-equipment">
      <div className="selected-equipment__header">
        <h3>Equipaggiamenti selezionati</h3>
      </div>

      <div className="selected-equipment__slots">
        {Array.from({ length: 5 }, (_, index) => {
          const card = equipment[index]
          return (
            <CardSlot
              key={`eq-slot-${index}`}
              type="equipment"
              card={card}
              onAdd={() => onAddSlot(index)}
              onRemove={() => onRemove(index)}
            />
          )
        })}
      </div>
    </div>
  )
}
