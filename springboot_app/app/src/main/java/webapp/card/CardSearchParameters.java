package webapp.card;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** Query-string parameters shared by card catalog and collection endpoints. */
public class CardSearchParameters {
    @Size(max = 100)
    private String name;

    @Size(max = 100)
    private String faction;

    @Size(max = 100)
    private String rarity;

    @Size(max = 100)
    private String type;

    @Size(max = 100)
    private String expansion;

    @Min(0)
    private Integer energyMin;

    @Min(0)
    private Integer energyMax;

    @Min(0)
    private Integer attackMin;

    @Min(0)
    private Integer attackMax;

    @Min(0)
    private Integer defenseMin;

    @Min(0)
    private Integer defenseMax;

    @Min(0)
    private Integer powerMin;

    @Min(0)
    private Integer powerMax;

    private String sort = "name";

    private String direction = "asc";

    @Min(0)
    private int page;

    @Min(1)
    @Max(100)
    private int size = 20;

    public CardSearchCriteria toCriteria() {
        return CardSearchCriteria.create(name, faction, rarity, type, expansion,
                energyMin, energyMax, attackMin, attackMax, defenseMin, defenseMax,
                powerMin, powerMax, sort, direction, page, size);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFaction() {
        return faction;
    }

    public void setFaction(String faction) {
        this.faction = faction;
    }

    public String getRarity() {
        return rarity;
    }

    public void setRarity(String rarity) {
        this.rarity = rarity;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getExpansion() {
        return expansion;
    }

    public void setExpansion(String expansion) {
        this.expansion = expansion;
    }

    public Integer getEnergyMin() {
        return energyMin;
    }

    public void setEnergyMin(Integer energyMin) {
        this.energyMin = energyMin;
    }

    public Integer getEnergyMax() {
        return energyMax;
    }

    public void setEnergyMax(Integer energyMax) {
        this.energyMax = energyMax;
    }

    public Integer getAttackMin() {
        return attackMin;
    }

    public void setAttackMin(Integer attackMin) {
        this.attackMin = attackMin;
    }

    public Integer getAttackMax() {
        return attackMax;
    }

    public void setAttackMax(Integer attackMax) {
        this.attackMax = attackMax;
    }

    public Integer getDefenseMin() {
        return defenseMin;
    }

    public void setDefenseMin(Integer defenseMin) {
        this.defenseMin = defenseMin;
    }

    public Integer getDefenseMax() {
        return defenseMax;
    }

    public void setDefenseMax(Integer defenseMax) {
        this.defenseMax = defenseMax;
    }

    public Integer getPowerMin() {
        return powerMin;
    }

    public void setPowerMin(Integer powerMin) {
        this.powerMin = powerMin;
    }

    public Integer getPowerMax() {
        return powerMax;
    }

    public void setPowerMax(Integer powerMax) {
        this.powerMax = powerMax;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
