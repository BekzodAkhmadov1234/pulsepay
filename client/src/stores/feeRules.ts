import { ref } from 'vue';
import { defineStore } from 'pinia';
import { feeRulesApi } from '@/lib/api/feeRules';
import type { FeeRuleResponse, CreateFeeRuleRequest, AddTierRequest } from '@/lib/api/feeRules';

export const useFeeRulesStore = defineStore('feeRules', () => {
  const rules = ref<FeeRuleResponse[]>([]);
  const isLoading = ref(false);
  const error = ref<string | null>(null);

  async function loadRules(): Promise<void> {
    isLoading.value = true;
    error.value = null;
    try {
      rules.value = await feeRulesApi.list();
    } catch (e: unknown) {
      error.value = e instanceof Error ? e.message : 'Failed to load fee rules';
    } finally {
      isLoading.value = false;
    }
  }

  async function createRule(data: CreateFeeRuleRequest): Promise<FeeRuleResponse> {
    const rule = await feeRulesApi.create(data);
    rules.value.unshift(rule);
    return rule;
  }

  async function activateRule(id: string): Promise<void> {
    const updated = await feeRulesApi.activate(id);
    const idx = rules.value.findIndex((r) => r.id === id);
    if (idx !== -1) rules.value[idx] = updated;
  }

  async function deactivateRule(id: string): Promise<void> {
    const updated = await feeRulesApi.deactivate(id);
    const idx = rules.value.findIndex((r) => r.id === id);
    if (idx !== -1) rules.value[idx] = updated;
  }

  async function supersedeRule(id: string, data: CreateFeeRuleRequest): Promise<FeeRuleResponse> {
    const newRule = await feeRulesApi.supersede(id, data);
    // Deactivate old rule in-place in the list
    const idx = rules.value.findIndex((r) => r.id === id);
    if (idx !== -1) rules.value[idx] = { ...rules.value[idx], isActive: false };
    rules.value.unshift(newRule);
    return newRule;
  }

  async function addTier(ruleId: string, data: AddTierRequest): Promise<void> {
    const tier = await feeRulesApi.addTier(ruleId, data);
    const rule = rules.value.find((r) => r.id === ruleId);
    if (rule) rule.tiers.push(tier);
  }

  async function removeTier(ruleId: string, tierId: string): Promise<void> {
    await feeRulesApi.removeTier(ruleId, tierId);
    const rule = rules.value.find((r) => r.id === ruleId);
    if (rule) rule.tiers = rule.tiers.filter((t) => t.id !== tierId);
  }

  return {
    rules,
    isLoading,
    error,
    loadRules,
    createRule,
    activateRule,
    deactivateRule,
    supersedeRule,
    addTier,
    removeTier,
  };
});
