import { Injectable, BadRequestException, ConflictException, ForbiddenException } from '@nestjs/common';
import { PrismaService } from '../../prisma/prisma.service';

@Injectable()
export class CreditsService {
  constructor(private readonly prisma: PrismaService) {}

  async adjustResellerCredits(params: {
    resellerId: string;
    type: 'CREDIT_ADD' | 'CREDIT_DEDUCT';
    amount: number;
    idempotencyKey: string;
    description: string;
    actorRole: string;
    actorId: string;
  }) {
    if (params.amount <= 0) {
      throw new BadRequestException('Amount must be positive');
    }

    return this.prisma.$transaction(async (tx) => {
      const existingTx = await tx.creditTransaction.findUnique({
        where: { idempotencyKey: params.idempotencyKey },
      });
      if (existingTx) {
        throw new ConflictException('Duplicate credit transaction blocked');
      }

      const reseller = await tx.reseller.findUnique({
        where: { id: params.resellerId },
        include: { wallet: true },
      });
      if (!reseller || reseller.deletedAt || !reseller.wallet) {
        throw new BadRequestException('Reseller wallet not found');
      }

      const delta = params.type === 'CREDIT_ADD' ? params.amount : -params.amount;
      const before = reseller.wallet.balanceCredits;
      const after = before + delta;

      if (params.type === 'CREDIT_ADD' && after > reseller.creditLimit) {
        throw new BadRequestException('Exceeds reseller credit limit');
      }
      if (after < 0 && !reseller.allowNegativeBalance) {
        throw new ForbiddenException('Negative reseller balance prohibited');
      }

      await tx.resellerWallet.update({
        where: { resellerId: params.resellerId },
        data: {
          balanceCredits: after,
          totalCredited: params.type === 'CREDIT_ADD' ? { increment: params.amount } : undefined,
          totalSpent: params.type === 'CREDIT_DEDUCT' ? { increment: params.amount } : undefined,
        },
      });

      const ledgerEntry = await tx.creditTransaction.create({
        data: {
          resellerId: params.resellerId,
          txType: params.type,
          amount: params.amount,
          balanceBefore: before,
          balanceAfter: after,
          idempotencyKey: params.idempotencyKey,
          description: params.description,
          performedByRole: params.actorRole,
          performedById: params.actorId,
        },
      });

      return { balanceCredits: after, transaction: ledgerEntry };
    });
  }
}
